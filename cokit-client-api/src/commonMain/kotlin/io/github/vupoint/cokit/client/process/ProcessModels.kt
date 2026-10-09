package io.github.vupoint.cokit.client.process

import io.github.vupoint.cokit.client.CodexHostPath
import io.github.vupoint.cokit.client.ExperimentalCodexApi
import kotlinx.serialization.Serializable

/** Caller-supplied connection-scoped handle for the experimental standalone process lifecycle. */
@ExperimentalCodexApi
@JvmInline
@Serializable
value class ProcessHandle(val value: String)

/** Experimental process PTY dimensions measured in character columns and rows. */
@ExperimentalCodexApi
@Serializable
data class ProcessTerminalSize(
    val cols: Int,
    val rows: Int,
)

/**
 * Spawns an experimental unsandboxed process on the app-server host. This surface requires explicit API opt-in and deliberate application authorization.
 *
 * @property command Argument vector; CoKit does not add shell interpolation.
 * @property processHandle Caller-supplied handle reused by stdin, kill, and PTY resize operations on this connection.
 * @property env Environment overrides; null map values are transmitted as JSON null.
 * @property outputBytesCap Optional output cap in bytes.
 * @property timeoutMs Optional process timeout in milliseconds.
 * @property size Optional terminal dimensions for PTY execution.
 */
@ExperimentalCodexApi
@Serializable
data class ProcessSpawnParams(
    val command: List<String>,
    val cwd: CodexHostPath,
    val processHandle: ProcessHandle,
    val env: Map<String, String?>? = null,
    val outputBytesCap: Long? = null,
    val timeoutMs: Long? = null,
    val tty: Boolean? = null,
    val size: ProcessTerminalSize? = null,
    val streamStdin: Boolean? = null,
    val streamStdoutStderr: Boolean? = null,
)

/**
 * Writes caller-encoded bytes to an experimental host process on this connection.
 *
 * @property deltaBase64 Base64-encoded input bytes; null supplies no input chunk.
 * @property closeStdin Optional request to close the process input stream.
 */
@ExperimentalCodexApi
@Serializable
data class ProcessWriteStdinParams(
    val processHandle: ProcessHandle,
    val deltaBase64: String? = null,
    val closeStdin: Boolean? = null,
)

/** Requests termination of an experimental host process identified on this connection. */
@ExperimentalCodexApi
@Serializable
data class ProcessKillParams(
    val processHandle: ProcessHandle,
)

/** Changes PTY dimensions for an experimental host process identified on this connection. */
@ExperimentalCodexApi
@Serializable
data class ProcessResizePtyParams(
    val processHandle: ProcessHandle,
    val size: ProcessTerminalSize,
)
