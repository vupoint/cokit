package io.github.vupoint.cokit.client.commands

import io.github.vupoint.cokit.client.CodexHostPath
import io.github.vupoint.cokit.client.SandboxPolicy
import kotlinx.serialization.Serializable

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

@Serializable
data class CommandExecResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

@Serializable
data class CommandExecWriteParams(
    val processId: CommandProcessId,
    val deltaBase64: String? = null,
    val closeStdin: Boolean? = null,
)

@Serializable
data class CommandExecResizeParams(
    val processId: CommandProcessId,
    val size: CommandExecTerminalSize,
)

@Serializable
data class CommandExecTerminateParams(
    val processId: CommandProcessId,
)

@Serializable
@JvmInline
value class CommandExecOutputStream(val value: String) {
    companion object {
        val Stdout = CommandExecOutputStream("stdout")
        val Stderr = CommandExecOutputStream("stderr")
    }
}

@Serializable
@JvmInline
value class CommandProcessId(val value: String)

@Serializable
data class CommandExecTerminalSize(
    val cols: Int,
    val rows: Int,
)

@Serializable
@JvmInline
value class CommandNetworkAccess(val value: String) {
    companion object {
        val Restricted = CommandNetworkAccess("restricted")
        val Enabled = CommandNetworkAccess("enabled")
    }
}
