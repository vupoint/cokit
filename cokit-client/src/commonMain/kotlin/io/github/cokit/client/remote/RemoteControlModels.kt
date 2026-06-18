package io.github.cokit.client.remote

import io.github.cokit.client.ExperimentalCodexApi
import kotlinx.serialization.Serializable

@ExperimentalCodexApi
@JvmInline
@Serializable
value class RemoteControlInstallationId(val value: String)

@ExperimentalCodexApi
@JvmInline
@Serializable
value class RemoteControlEnvironmentId(val value: String)

@ExperimentalCodexApi
@JvmInline
@Serializable
value class RemoteControlConnectionStatus(val value: String) {
    companion object {
        val Disabled = RemoteControlConnectionStatus("disabled")
        val Connecting = RemoteControlConnectionStatus("connecting")
        val Connected = RemoteControlConnectionStatus("connected")
        val Errored = RemoteControlConnectionStatus("errored")
    }
}

@ExperimentalCodexApi
@Serializable
data class RemoteControlStatusSnapshot(
    val status: RemoteControlConnectionStatus,
    val installationId: RemoteControlInstallationId,
    val serverName: String,
    val environmentId: RemoteControlEnvironmentId? = null,
)

@ExperimentalCodexApi
@Serializable
data class RemoteControlEnableParams(
    val ephemeral: Boolean? = null,
)

@ExperimentalCodexApi
@Serializable
data class RemoteControlDisableParams(
    val ephemeral: Boolean? = null,
)

@ExperimentalCodexApi
@Serializable
data object RemoteControlStatusReadParams
