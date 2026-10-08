package io.github.vupoint.cokit.client.remote

import io.github.vupoint.cokit.client.CodexCursor
import io.github.vupoint.cokit.client.ExperimentalCodexApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Experimental remote-control installation identifier; treat it as private host metadata. */
@ExperimentalCodexApi
@JvmInline
@Serializable
value class RemoteControlInstallationId(val value: String)

/** Experimental remote-control environment identifier used for pairing and controller management. */
@ExperimentalCodexApi
@JvmInline
@Serializable
value class RemoteControlEnvironmentId(val value: String)

/** Experimental server remote-control connection state; unknown wire strings are retained. */
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

/**
 * Experimental remote-control state reported by app-server. Identifiers and host names are private metadata; reading this state does not enable access.
 *
 * @property environmentId Optional environment identity; absence is not evidence of a connected controller.
 */
@ExperimentalCodexApi
@Serializable
data class RemoteControlStatusSnapshot(
    val status: RemoteControlConnectionStatus,
    val installationId: RemoteControlInstallationId,
    val serverName: String,
    val environmentId: RemoteControlEnvironmentId? = null,
)

/**
 * Explicit experimental request to enable app-server remote control. Applications own remote-access policy and must opt into the experimental descriptor.
 *
 * @property ephemeral Optional request for a process-only change; null uses the server default.
 */
@ExperimentalCodexApi
@Serializable
data class RemoteControlEnableParams(
    val ephemeral: Boolean? = null,
)

/**
 * Explicit experimental request to disable app-server remote control.
 *
 * @property ephemeral Optional request for a process-only change; null uses the server default.
 */
@ExperimentalCodexApi
@Serializable
data class RemoteControlDisableParams(
    val ephemeral: Boolean? = null,
)

/** Empty parameters for reading experimental remote-control connection state. */
@ExperimentalCodexApi
@Serializable
data object RemoteControlStatusReadParams

/**
 * Private experimental pairing artifact; its string representation is redacted, but [value] and serialization expose the original code.
 */
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

/** Serializes the original pairing code as a wire string; serialization is not redacted. */
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

/** Private experimental user-entered pairing code; string rendering is redacted, while [value] remains sensitive. */
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

/** Serializes the original manual pairing code as a wire string; serialization is not redacted. */
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

/** Private experimental controller identity used for revocation; its string representation is redacted. */
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

/** Serializes the original remote controller id as a wire string; serialization is not redacted. */
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

/**
 * Starts explicit experimental controller pairing.
 *
 * @property manualCode Optional request to include a user-entered pairing code.
 */
@ExperimentalCodexApi
@Serializable
data class RemoteControlPairingStartParams(
    val manualCode: Boolean? = null,
)

/**
 * Short-lived experimental pairing artifacts and server-reported expiry. Raw codes must be handled as sensitive data and displayed only for the intended pairing flow.
 */
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

/**
 * Queries experimental pairing claim status using a pairing artifact; raw codes remain sensitive even though string rendering is redacted.
 */
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

/** Whether the server reports the selected experimental pairing artifact as claimed. */
@ExperimentalCodexApi
@Serializable
data class RemoteControlPairingStatusResult(
    val claimed: Boolean,
)

/** Experimental controller-list ordering selector; unknown wire strings are retained. */
@ExperimentalCodexApi
@JvmInline
@Serializable
value class RemoteControlClientsListOrder(val value: String) {
    companion object {
        val Asc = RemoteControlClientsListOrder("asc")
        val Desc = RemoteControlClientsListOrder("desc")
    }
}

/** Lists experimental remote controllers for one environment with optional server-controlled paging and ordering. */
@ExperimentalCodexApi
@Serializable
data class RemoteControlClientsListParams(
    val environmentId: RemoteControlEnvironmentId,
    val cursor: CodexCursor? = null,
    val limit: Int? = null,
    val order: RemoteControlClientsListOrder? = null,
)

/** One page of experimental controller metadata; null [nextCursor] means no continuation was reported. */
@ExperimentalCodexApi
@Serializable
data class RemoteControlClientsListResult(
    val data: List<RemoteControlClient> = emptyList(),
    val nextCursor: CodexCursor? = null,
)

/**
 * Experimental remote-controller identity and optional device metadata. String rendering redacts the id but other reported device fields may still be private.
 */
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

/** Explicit experimental revocation of one controller in an environment; applications must authorize the access change. */
@ExperimentalCodexApi
@Serializable
data class RemoteControlClientsRevokeParams(
    val environmentId: RemoteControlEnvironmentId,
    val clientId: RemoteControlClientId,
) {
    override fun toString(): String =
        "RemoteControlClientsRevokeParams(environmentId=$environmentId, clientId=<redacted>)"
}

/** Empty acknowledgment of an experimental controller revocation request. */
@ExperimentalCodexApi
@Serializable
data object RemoteControlClientsRevokeResult
