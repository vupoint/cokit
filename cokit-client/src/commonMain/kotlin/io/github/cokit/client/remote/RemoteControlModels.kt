package io.github.cokit.client.remote

import io.github.cokit.client.CodexCursor
import io.github.cokit.client.ExperimentalCodexApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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

@ExperimentalCodexApi
@Serializable(with = RemoteControlPairingCodeSerializer::class)
class RemoteControlPairingCode(
    val value: String,
) {
    override fun equals(other: Any?): Boolean =
        other is RemoteControlPairingCode && value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "<redacted>"
}

@ExperimentalCodexApi
object RemoteControlPairingCodeSerializer : KSerializer<RemoteControlPairingCode> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("RemoteControlPairingCode", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): RemoteControlPairingCode =
        RemoteControlPairingCode(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: RemoteControlPairingCode) {
        encoder.encodeString(value.value)
    }
}

@ExperimentalCodexApi
@Serializable(with = RemoteControlManualPairingCodeSerializer::class)
class RemoteControlManualPairingCode(
    val value: String,
) {
    override fun equals(other: Any?): Boolean =
        other is RemoteControlManualPairingCode && value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "<redacted>"
}

@ExperimentalCodexApi
object RemoteControlManualPairingCodeSerializer : KSerializer<RemoteControlManualPairingCode> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("RemoteControlManualPairingCode", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): RemoteControlManualPairingCode =
        RemoteControlManualPairingCode(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: RemoteControlManualPairingCode) {
        encoder.encodeString(value.value)
    }
}

@ExperimentalCodexApi
@Serializable(with = RemoteControlClientIdSerializer::class)
class RemoteControlClientId(
    val value: String,
) {
    override fun equals(other: Any?): Boolean =
        other is RemoteControlClientId && value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "<redacted>"
}

@ExperimentalCodexApi
object RemoteControlClientIdSerializer : KSerializer<RemoteControlClientId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("RemoteControlClientId", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): RemoteControlClientId =
        RemoteControlClientId(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: RemoteControlClientId) {
        encoder.encodeString(value.value)
    }
}

@ExperimentalCodexApi
@Serializable
data class RemoteControlPairingStartParams(
    val manualCode: Boolean? = null,
)

@ExperimentalCodexApi
@Serializable
data class RemoteControlPairingStartResult(
    val pairingCode: RemoteControlPairingCode,
    val manualPairingCode: RemoteControlManualPairingCode? = null,
    val environmentId: RemoteControlEnvironmentId,
    val expiresAt: Long,
) {
    override fun toString(): String =
        "RemoteControlPairingStartResult(pairingCode=<redacted>, " +
            "manualPairingCode=${if (manualPairingCode == null) null else "<redacted>"}, " +
            "environmentId=$environmentId, expiresAt=$expiresAt)"
}

@ExperimentalCodexApi
@Serializable
data class RemoteControlPairingStatusParams(
    val pairingCode: RemoteControlPairingCode? = null,
    val manualPairingCode: RemoteControlManualPairingCode? = null,
) {
    override fun toString(): String =
        "RemoteControlPairingStatusParams(" +
            "pairingCode=${if (pairingCode == null) null else "<redacted>"}, " +
            "manualPairingCode=${if (manualPairingCode == null) null else "<redacted>"})"
}

@ExperimentalCodexApi
@Serializable
data class RemoteControlPairingStatusResult(
    val claimed: Boolean,
)

@ExperimentalCodexApi
@JvmInline
@Serializable
value class RemoteControlClientsListOrder(val value: String) {
    companion object {
        val Asc = RemoteControlClientsListOrder("asc")
        val Desc = RemoteControlClientsListOrder("desc")
    }
}

@ExperimentalCodexApi
@Serializable
data class RemoteControlClientsListParams(
    val environmentId: RemoteControlEnvironmentId,
    val cursor: CodexCursor? = null,
    val limit: Int? = null,
    val order: RemoteControlClientsListOrder? = null,
)

@ExperimentalCodexApi
@Serializable
data class RemoteControlClientsListResult(
    val data: List<RemoteControlClient> = emptyList(),
    val nextCursor: CodexCursor? = null,
)

@ExperimentalCodexApi
@Serializable
data class RemoteControlClient(
    val clientId: RemoteControlClientId,
    val displayName: String? = null,
    val deviceType: String? = null,
    val platform: String? = null,
    val osVersion: String? = null,
    val deviceModel: String? = null,
    val appVersion: String? = null,
    val lastSeenAt: Long? = null,
) {
    override fun toString(): String =
        "RemoteControlClient(clientId=<redacted>, displayName=$displayName, " +
            "deviceType=$deviceType, platform=$platform, osVersion=$osVersion, " +
            "deviceModel=$deviceModel, appVersion=$appVersion, lastSeenAt=$lastSeenAt)"
}

@ExperimentalCodexApi
@Serializable
data class RemoteControlClientsRevokeParams(
    val environmentId: RemoteControlEnvironmentId,
    val clientId: RemoteControlClientId,
) {
    override fun toString(): String =
        "RemoteControlClientsRevokeParams(environmentId=$environmentId, clientId=<redacted>)"
}

@ExperimentalCodexApi
@Serializable
data object RemoteControlClientsRevokeResult
