package io.github.vupoint.cokit.client.commands

import io.github.vupoint.cokit.client.CodexHostPath
import io.github.vupoint.cokit.client.SandboxPolicy
import kotlinx.serialization.Serializable

/**
 * Arguments for command execution on the app-server host under the selected or default sandbox policy.
 * [command] is an argv vector; CoKit does not interpolate it through a shell. Null options leave server defaults in effect.
 *
 * @property env Environment overrides; null map values are transmitted as JSON null.
 * @property outputBytesCap Optional cap on captured output in bytes.
 * @property timeoutMs Optional execution timeout in milliseconds.
 * @property disableOutputCap Explicit request to disable the server's output cap.
 * @property disableTimeout Explicit request to disable the server's execution timeout.
 * @property processId Caller-supplied connection-scoped id for later stdin, resize, and terminate operations.
 * @property streamStdoutStderr Requests output notifications as execution proceeds.
 * @property sandboxPolicy Host execution policy, evaluated by app-server.
 */
@Serializable
data class CommandExecParams(
    val command: List<String>,
    val cwd: CodexHostPath? = null,
    val disableOutputCap: Boolean? = null,
    val disableTimeout: Boolean? = null,
    val env: Map<String, String?>? = null,
    val outputBytesCap: Long? = null,
    val tty: Boolean? = null,
    val processId: CommandProcessId? = null,
    val sandboxPolicy: SandboxPolicy? = null,
    val size: CommandExecTerminalSize? = null,
    val streamStdin: Boolean? = null,
    val streamStdoutStderr: Boolean? = null,
    val timeoutMs: Long? = null,
)

/** Buffered command completion result. A nonzero [exitCode] reports process failure even when the JSON-RPC request succeeded. */
@Serializable
data class CommandExecResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

/**
 * Writes caller-encoded stdin bytes to a command process on this connection.
 *
 * @property deltaBase64 Base64-encoded input bytes; null supplies no input chunk.
 * @property closeStdin Optional request to close the process input stream.
 */
@Serializable
data class CommandExecWriteParams(
    val processId: CommandProcessId,
    val deltaBase64: String? = null,
    val closeStdin: Boolean? = null,
)

/** Changes the PTY dimensions of a command process on this connection. */
@Serializable
data class CommandExecResizeParams(
    val processId: CommandProcessId,
    val size: CommandExecTerminalSize,
)

/** Requests termination of the command process identified on this connection. */
@Serializable
data class CommandExecTerminateParams(
    val processId: CommandProcessId,
)

/** Output channel associated with a command execution delta; unknown strings are retained. */
@Serializable
@JvmInline
value class CommandExecOutputStream(val value: String) {
    companion object {
        val Stdout = CommandExecOutputStream("stdout")
        val Stderr = CommandExecOutputStream("stderr")
    }
}

/** Caller-supplied connection-scoped command process id, reused for stdin, resize, and termination. */
@Serializable
@JvmInline
value class CommandProcessId(val value: String)

/** PTY dimensions measured in character columns and rows, rather than pixels. */
@Serializable
data class CommandExecTerminalSize(
    val cols: Int,
    val rows: Int,
)

/** Network access mode used by host command policies; unknown wire strings are retained. */
@Serializable
@JvmInline
value class CommandNetworkAccess(val value: String) {
    companion object {
        val Restricted = CommandNetworkAccess("restricted")
        val Enabled = CommandNetworkAccess("enabled")
    }
}
