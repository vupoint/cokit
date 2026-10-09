package io.github.vupoint.cokit.client.config

import io.github.vupoint.cokit.client.CodexHostPath
import io.github.vupoint.cokit.client.CodexJsonPayload
import io.github.vupoint.cokit.client.toCodexPayload
import io.github.vupoint.cokit.client.toJsonElement
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonElement

/** Key path interpreted by app-server when editing configuration; CoKit passes the original text without resolving it locally. */
@Serializable
@JvmInline
value class ConfigKeyPath(val value: String)

/** Server merge operation for a config edit: replace the target or upsert into it; unknown strings are retained. */
@Serializable
@JvmInline
value class ConfigMergeStrategy(val value: String) {
    companion object {
        val Replace = ConfigMergeStrategy("replace")
        val Upsert = ConfigMergeStrategy("upsert")
    }
}

/** Whether a config write is effective or overridden by a higher-priority layer; unknown strings are retained. */
@Serializable
@JvmInline
value class ConfigWriteStatus(val value: String) {
    companion object {
        val Ok = ConfigWriteStatus("ok")
        val OkOverridden = ConfigWriteStatus("okOverridden")
    }
}

/**
 * Arbitrary JSON configuration preserved without a fixed Kotlin schema. Its string representation includes the payload and may contain private values.
 */
@Serializable(with = ConfigValueSerializer::class)
class ConfigValue internal constructor(
    val payload: CodexJsonPayload,
) {
    /** Returns the complete JSON value without redaction; avoid logging private configuration. */
    fun toJsonString(): String = payload.toJsonString()

    override fun equals(other: Any?): Boolean =
        other is ConfigValue && payload == other.payload

    override fun hashCode(): Int = payload.hashCode()

    override fun toString(): String = toJsonString()

    companion object {
        /** Parses arbitrary JSON without imposing a configuration schema; malformed JSON fails parsing. */
        fun parse(json: String): ConfigValue = ConfigValue(CodexJsonPayload.parse(json))
    }
}

/** JSON-only serializer preserving a configuration value as its original JSON shape. */
object ConfigValueSerializer : KSerializer<ConfigValue> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): ConfigValue {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("ConfigValue requires JSON decoding")
        return ConfigValue(jsonDecoder.decodeJsonElement().toCodexPayload())
    }

    override fun serialize(encoder: Encoder, value: ConfigValue) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("ConfigValue requires JSON encoding")
        jsonEncoder.encodeJsonElement(
            value.payload.toJsonElement()
                ?: throw SerializationException("ConfigValue payload cannot be null"),
        )
    }
}

/**
 * Reads effective app-server configuration for an optional host working directory.
 *
 * @property includeLayers Requests source-layer payloads in addition to the effective configuration.
 */
@Serializable
data class ConfigReadParams(
    val cwd: CodexHostPath? = null,
    val includeLayers: Boolean? = null,
)

/**
 * Effective configuration and its provenance.
 *
 * @property origins Source metadata for server-reported keys.
 * @property layers Optional layer payloads requested with includeLayers.
 */
@Serializable
data class ConfigReadResult(
    val config: ConfigValue,
    val origins: Map<String, ConfigLayerMetadata> = emptyMap(),
    val layers: List<ConfigLayer>? = null,
)

/**
 * Edits one key in an app-server host configuration file.
 *
 * @property filePath Optional explicit target; null uses the file selected by app-server.
 * @property expectedVersion Optional optimistic-concurrency version for the target file.
 */
@Serializable
data class ConfigValueWriteParams(
    val keyPath: ConfigKeyPath,
    val value: ConfigValue,
    val mergeStrategy: ConfigMergeStrategy,
    val filePath: CodexHostPath? = null,
    val expectedVersion: String? = null,
)

/**
 * Applies a batch of config edits through app-server.
 *
 * @property filePath Optional explicit target; null uses the file selected by app-server.
 * @property expectedVersion Optional optimistic-concurrency version for the target file.
 * @property reloadUserConfig Requests reloading updated user config into loaded threads after writing.
 */
@Serializable
data class ConfigBatchWriteParams(
    val edits: List<ConfigEdit>,
    val filePath: CodexHostPath? = null,
    val expectedVersion: String? = null,
    val reloadUserConfig: Boolean? = null,
)

/** One key, value, and merge operation in a configuration batch. */
@Serializable
data class ConfigEdit(
    val keyPath: ConfigKeyPath,
    val value: ConfigValue,
    val mergeStrategy: ConfigMergeStrategy,
)

/** Written host file and new version, including metadata when a higher-priority layer overrides the edit. */
@Serializable
data class ConfigWriteResult(
    val filePath: CodexHostPath,
    val status: ConfigWriteStatus,
    val version: String,
    val overriddenMetadata: ConfigOverriddenMetadata? = null,
)

/** Explains why a written config value is not effective, including the winning value and layer. */
@Serializable
data class ConfigOverriddenMetadata(
    val effectiveValue: ConfigValue,
    val message: String,
    val overridingLayer: ConfigLayerMetadata,
)

/**
 * One app-server configuration layer, its version, and raw values.
 *
 * @property disabledReason Optional explanation of why the server disabled this layer.
 */
@Serializable
data class ConfigLayer(
    val name: ConfigLayerSource,
    val version: String,
    val config: ConfigValue,
    val disabledReason: String? = null,
)

/** Source and version identifying a configuration layer without including its values. */
@Serializable
data class ConfigLayerMetadata(
    val name: ConfigLayerSource,
    val version: String,
)

/** Origin of configuration reported by app-server, including host files, managed policy, and session flags. */
@Serializable
sealed interface ConfigLayerSource {
    /** Managed-device configuration source identified by domain and key. */
    @Serializable
    @SerialName("mdm")
    data class Mdm(
        val domain: String,
        val key: String,
    ) : ConfigLayerSource

    /** System configuration file on the app-server host. */
    @Serializable
    @SerialName("system")
    data class SystemFile(
        val file: CodexHostPath,
    ) : ConfigLayerSource

    /** Enterprise-managed configuration source identified by the server. */
    @Serializable
    @SerialName("enterpriseManaged")
    data class EnterpriseManaged(
        val id: String,
        val name: String,
    ) : ConfigLayerSource

    /** User configuration file on the app-server host, optionally selecting a profile. */
    @Serializable
    @SerialName("user")
    data class User(
        val file: CodexHostPath,
        val profile: String? = null,
    ) : ConfigLayerSource

    /** Project configuration located under the reported host Codex folder. */
    @Serializable
    @SerialName("project")
    data class Project(
        val dotCodexFolder: CodexHostPath,
    ) : ConfigLayerSource

    /** Configuration supplied by app-server session flags. */
    @Serializable
    @SerialName("sessionFlags")
    data object SessionFlags : ConfigLayerSource

    /** Legacy managed configuration file source retained for older servers. */
    @Serializable
    @SerialName("legacyManagedConfigTomlFromFile")
    data class LegacyManagedConfigTomlFromFile(
        val file: CodexHostPath,
    ) : ConfigLayerSource

    /** Legacy managed-device configuration source retained for older servers. */
    @Serializable
    @SerialName("legacyManagedConfigTomlFromMdm")
    data object LegacyManagedConfigTomlFromMdm : ConfigLayerSource
}
