package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.commands.CommandNetworkAccess
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
@JvmInline
value class CodexHostPath(val value: String)

@Serializable
@JvmInline
value class ModelName(val value: String)

@Serializable(with = ApprovalPolicySerializer::class)
sealed interface ApprovalPolicy {
    data object Untrusted : ApprovalPolicy

    @Deprecated("on-failure is a legacy compatibility value and is not part of the current stable schema.")
    data object OnFailure : ApprovalPolicy

    data object OnRequest : ApprovalPolicy

    data object Never : ApprovalPolicy

    data class Granular(
        val granular: GranularApprovalPolicy,
    ) : ApprovalPolicy

    data class Custom(
        val value: String,
    ) : ApprovalPolicy
}

@Serializable
@JvmInline
value class SandboxMode(val value: String) {
    companion object {
        val ReadOnly = SandboxMode("read-only")
        val WorkspaceWrite = SandboxMode("workspace-write")
        val DangerFullAccess = SandboxMode("danger-full-access")
    }
}

@Serializable
data class GranularApprovalPolicy(
    @SerialName("mcp_elicitations")
    val mcpElicitations: Boolean,
    val rules: Boolean,
    @SerialName("sandbox_approval")
    val sandboxApproval: Boolean,
    @SerialName("request_permissions")
    val requestPermissions: Boolean = false,
    @SerialName("skill_approval")
    val skillApproval: Boolean = false,
)

internal object ApprovalPolicySerializer : KSerializer<ApprovalPolicy> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): ApprovalPolicy {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("ApprovalPolicy requires JSON decoding")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> decodeStringPolicy(element.content)
            is JsonObject -> {
                val envelope = jsonDecoder.json.decodeFromJsonElement<GranularApprovalPolicyEnvelope>(element)
                ApprovalPolicy.Granular(envelope.granular)
            }
            else -> throw SerializationException("ApprovalPolicy must be a string or granular object")
        }
    }

    @Suppress("DEPRECATION")
    override fun serialize(encoder: Encoder, value: ApprovalPolicy) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("ApprovalPolicy requires JSON encoding")
        val element = when (value) {
            ApprovalPolicy.Untrusted -> JsonPrimitive("untrusted")
            ApprovalPolicy.OnFailure -> JsonPrimitive("on-failure")
            ApprovalPolicy.OnRequest -> JsonPrimitive("on-request")
            ApprovalPolicy.Never -> JsonPrimitive("never")
            is ApprovalPolicy.Custom -> JsonPrimitive(value.value)
            is ApprovalPolicy.Granular -> jsonEncoder.json.encodeToJsonElement(
                GranularApprovalPolicyEnvelope.serializer(),
                GranularApprovalPolicyEnvelope(value.granular),
            )
        }
        jsonEncoder.encodeJsonElement(element)
    }

    @Suppress("DEPRECATION")
    private fun decodeStringPolicy(value: String): ApprovalPolicy = when (value) {
        "untrusted" -> ApprovalPolicy.Untrusted
        "on-failure" -> ApprovalPolicy.OnFailure
        "on-request" -> ApprovalPolicy.OnRequest
        "never" -> ApprovalPolicy.Never
        else -> ApprovalPolicy.Custom(value)
    }
}

@Serializable
private data class GranularApprovalPolicyEnvelope(
    val granular: GranularApprovalPolicy,
)

@Serializable
sealed interface SandboxPolicy {
    @Serializable
    @SerialName("dangerFullAccess")
    data object DangerFullAccess : SandboxPolicy

    @Serializable
    @SerialName("readOnly")
    data class ReadOnly(
        val networkAccess: Boolean? = null,
    ) : SandboxPolicy

    @Serializable
    @SerialName("externalSandbox")
    data class ExternalSandbox(
        val networkAccess: CommandNetworkAccess? = null,
    ) : SandboxPolicy

    @Serializable
    @SerialName("workspaceWrite")
    data class WorkspaceWrite(
        val writableRoots: List<CodexHostPath> = emptyList(),
        val networkAccess: Boolean? = null,
        val excludeTmpdirEnvVar: Boolean? = null,
        val excludeSlashTmp: Boolean? = null,
    ) : SandboxPolicy
}

@Serializable
@JvmInline
value class ReasoningEffort(val value: String) {
    companion object {
        val Low = ReasoningEffort("low")
        val Medium = ReasoningEffort("medium")
        val High = ReasoningEffort("high")
    }
}
