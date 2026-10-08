package io.github.vupoint.cokit.transport.stdio

import io.github.vupoint.cokit.protocol.CodexProtocolJson
import io.github.vupoint.cokit.protocol.JsonRpcMessage
import io.github.vupoint.cokit.rpc.JsonRpcTransport
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal val DefaultCodexAppServerStdioCommand = listOf("codex", "app-server", "--stdio")

/**
 * JVM transport using one UTF-8 JSON envelope per line over a child process's standard streams.
 *
 * Public construction starts the process immediately and owns its streams and shutdown.
 * Standard error is drained and discarded to avoid blocking the process. Incoming EOF or
 * decode/read failure does not complete [incoming] or deliver an error through that flow;
 * reader failures are reported through the reader coroutine's exception handling.
 * Raw lines are read and decoded without a transport-level size limit.
 *
 * @property command Executable and argument list used to start the process, without shell expansion.
 */
class StdioCodexTransport internal constructor(
    val command: List<String>,
    input: InputStream,
    output: OutputStream,
    scope: CoroutineScope,
    error: InputStream? = null,
    onClose: () -> Unit = {},
) : JsonRpcTransport {
    private val delegate = StreamStdioTransport(
        input = input,
        output = output,
        error = error,
        scope = scope,
        onClose = onClose,
    )

    /**
     * Starts a local process directly with [ProcessBuilder], without invoking a shell.
     *
     * The executable is resolved by the platform; choose a trusted command and working directory.
     * Environment values are merged into the inherited process environment rather than replacing
     * it. This constructor does not sanitize executable paths, arguments, or environment values.
     *
     * @param command Executable followed by arguments; defaults to `codex app-server --stdio`.
     * @param cwd Working directory, or `null` to inherit the current process directory.
     * @param env Environment overrides; empty by default, retaining all inherited variables.
     * @throws java.io.IOException If process creation fails.
     */
    constructor(
        command: List<String> = DefaultCodexAppServerStdioCommand,
        cwd: File? = null,
        env: Map<String, String> = emptyMap(),
    ) : this(command, startProcess(command, cwd, env))

    internal constructor(
        input: InputStream,
        output: OutputStream,
        scope: CoroutineScope,
        error: InputStream? = null,
    ) : this(
        command = emptyList(),
        input = input,
        output = output,
        scope = scope,
        error = error,
    )

    private constructor(
        command: List<String>,
        process: Process,
    ) : this(
        command = command,
        input = process.inputStream,
        output = process.outputStream,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        error = process.errorStream,
        onClose = { process.destroy() },
    )

    /**
     * Hot decoded message stream replaying the last 64 messages to new subscribers, with
     * capacity for 64 additional messages for slow subscribers and oldest-message dropping
     * on overflow. The flow remains open after EOF, reader failure, or [close].
     */
    override val incoming: SharedFlow<JsonRpcMessage> = delegate.incoming

    /**
     * Serializes, writes, and flushes one newline-delimited envelope under a write mutex.
     * Serialization and stream write failures propagate to the caller; no retry is performed.
     */
    override suspend fun send(message: JsonRpcMessage) {
        delegate.send(message)
    }

    /**
     * Cancels stream reader jobs, closes owned streams, and requests child termination with
     * [Process.destroy]. Stream-close failures are suppressed. This does not wait for process
     * exit or force termination, and repeated calls do nothing.
     */
    override fun close() {
        delegate.close()
    }

    /** Factory for a local app-server proxy process. */
    companion object {
        /**
         * Starts `codex app-server proxy` with an optional socket path passed as one argument.
         * The command inherits the working directory and environment; no shell expansion occurs.
         * Callers must select a trusted local executable and socket endpoint.
         *
         * @param sockPath Socket path argument, or `null` to use the proxy's default endpoint.
         * @return Transport owning the newly started proxy process.
         * @throws java.io.IOException If process creation fails.
         */
        fun proxy(sockPath: String? = null): StdioCodexTransport {
            val command = buildList {
                add("codex")
                add("app-server")
                add("proxy")
                sockPath?.let { add(it) }
            }
            return StdioCodexTransport(command)
        }

        private fun startProcess(
            command: List<String>,
            cwd: File?,
            env: Map<String, String>,
        ): Process {
            return ProcessBuilder(command)
                .also { builder ->
                    cwd?.let { builder.directory(it) }
                    builder.environment().putAll(env)
                }
                .start()
        }
    }
}

private class StreamStdioTransport(
    input: InputStream,
    output: OutputStream,
    private val error: InputStream? = null,
    scope: CoroutineScope,
    private val onClose: () -> Unit = {},
) : JsonRpcTransport {
    private val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
    private val writer = BufferedWriter(OutputStreamWriter(output, Charsets.UTF_8))
    private val writeMutex = Mutex()
    private var closed = false
    private val mutableIncoming = MutableSharedFlow<JsonRpcMessage>(
        replay = 64,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val readJob: Job = scope.launch {
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isNotBlank()) {
                mutableIncoming.emit(CodexProtocolJson.decodeFromString<JsonRpcMessage>(line))
            }
        }
    }
    private val errorJob: Job? = error?.let { stream ->
        scope.launch {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (stream.read(buffer) != -1) {
                // Drain stderr so app-server diagnostics cannot block stdout.
            }
        }
    }

    override val incoming: SharedFlow<JsonRpcMessage> = mutableIncoming

    override suspend fun send(message: JsonRpcMessage) {
        val line = CodexProtocolJson.encodeToString(message)
        writeMutex.withLock {
            writer.write(line)
            writer.newLine()
            writer.flush()
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        readJob.cancel()
        errorJob?.cancel()
        runCatching { writer.close() }
        runCatching { reader.close() }
        runCatching { error?.close() }
        onClose()
    }
}
