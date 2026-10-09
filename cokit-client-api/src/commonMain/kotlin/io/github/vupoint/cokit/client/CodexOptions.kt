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

/** A filesystem path interpreted on the app-server host, which may differ from the client host. */
@Serializable
@JvmInline
value class CodexHostPath(val value: String)

/** Server model identifier. Arbitrary strings allow model names beyond the known catalog. */
@Serializable
@JvmInline
value class ModelName(val value: String)

/**
 * Server policy for deciding when approval is requested.
 *
 * Selecting a policy does not register a CoKit approval handler or grant an incoming request.
 */
@Serializable(with = ApprovalPolicySerializer::class)
sealed interface ApprovalPolicy {
    /** Requests approval according to the server's untrusted-command policy. */
    data object Untrusted : ApprovalPolicy

    /** Legacy policy that requests approval after a sandboxed command fails. */
    @Deprecated("on-failure is a legacy compatibility value and is not part of the current stable schema.")
    data object OnFailure : ApprovalPolicy

    /** Allows the agent to ask for approval when it requires escalation. */
    data object OnRequest : ApprovalPolicy

    /** Disables interactive approval requests; it does not disable the sandbox. */
    data object Never : ApprovalPolicy

    /** Selects approval categories independently using [granular]. */
    data class Granular(
        val granular: GranularApprovalPolicy,
    ) : ApprovalPolicy

    /** Retains an unrecognized policy string for forward compatibility; server support is required. */
    data class Custom(
        val value: String,
    ) : ApprovalPolicy
}

/**
 * String-valued sandbox preset for thread configuration.
 *
 * Known presets are available as companion properties; arbitrary strings are passed to the server.
 */
@Serializable
@JvmInline
value class SandboxMode(val value: String) {
    companion object {
        val ReadOnly = SandboxMode("read-only")
        val WorkspaceWrite = SandboxMode("workspace-write")
        val DangerFullAccess = SandboxMode("danger-full-access")
    }
}

/** Selects the server-side reviewer for approval requests, independently of CoKit handlers. */
@Serializable
@JvmInline
value class ApprovalsReviewer(val value: String) {
    companion object {
        val User = ApprovalsReviewer("user")
        val AutoReview = ApprovalsReviewer("auto_review")

        @Deprecated("guardian_subagent is a legacy compatibility value. Use AutoReview.")
        val GuardianSubagent = ApprovalsReviewer("guardian_subagent")
    }
}

/** Server personality setting; strings are preserved for protocol compatibility. */
@Serializable
@JvmInline
value class Personality(val value: String) {
    companion object {
        val None = Personality("none")
        @Deprecated("Codex 0.157.1 no longer selects a style with friendly")
        val Friendly = Personality("friendly")
        @Deprecated("Codex 0.157.1 no longer selects a style with pragmatic")
        val Pragmatic = Personality("pragmatic")
    }
}

/** Requested presentation level for reasoning summaries, subject to model support. */
@Serializable
@JvmInline
value class ReasoningSummary(val value: String) {
    companion object {
        val Auto = ReasoningSummary("auto")
        val Concise = ReasoningSummary("concise")
        val Detailed = ReasoningSummary("detailed")
        val None = ReasoningSummary("none")
    }
}

/** Server service-tier identifier; availability and accepted values depend on the backend. */
@Serializable
@JvmInline
value class ServiceTier(val value: String)

/**
 * Approval categories enabled by the granular server policy.
 *
 * Each boolean controls its corresponding upstream category; omitted optional categories default to false.
 * These settings govern prompting and do not supply a response to any approval request.
 */
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

/**
 * Detailed server sandbox policy, used in results and per-turn overrides.
 *
 * Paths and network settings apply on the app-server host.
 */
@Serializable
sealed interface SandboxPolicy {
    /** Runs without the restrictions of the read-only or workspace-write sandbox presets. */
    @Serializable
    @SerialName("dangerFullAccess")
    data object DangerFullAccess : SandboxPolicy

    /** Read-only filesystem policy with an optional network-access setting. */
    @Serializable
    @SerialName("readOnly")
    data class ReadOnly(
        val networkAccess: Boolean? = null,
    ) : SandboxPolicy

    /** Declares that sandbox enforcement is provided outside app-server. */
    @Serializable
    @SerialName("externalSandbox")
    data class ExternalSandbox(
        val networkAccess: CommandNetworkAccess? = null,
    ) : SandboxPolicy

    /**
     * Permits writes to the server workspace and [writableRoots].
     *
     * @property networkAccess Optional network permission.
     * @property excludeTmpdirEnvVar Excludes the host `TMPDIR` from additional writable roots when true.
     * @property excludeSlashTmp Excludes the host `/tmp` from additional writable roots when true.
     */
    @Serializable
    @SerialName("workspaceWrite")
    data class WorkspaceWrite(
        val writableRoots: List<CodexHostPath> = emptyList(),
        val networkAccess: Boolean? = null,
        val excludeTmpdirEnvVar: Boolean? = null,
        val excludeSlashTmp: Boolean? = null,
    ) : SandboxPolicy
}

/** Model reasoning-effort identifier. Known levels are conveniences, not an exhaustive value set. */
@Serializable
@JvmInline
value class ReasoningEffort(val value: String) {
    companion object {
        val Low = ReasoningEffort("low")
        val Medium = ReasoningEffort("medium")
        val High = ReasoningEffort("high")
    }
}
