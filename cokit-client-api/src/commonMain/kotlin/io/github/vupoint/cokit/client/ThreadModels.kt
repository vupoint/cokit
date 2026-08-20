package io.github.vupoint.cokit.client

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
@JvmInline
value class ThreadId(val value: String)

@Serializable
@JvmInline
value class CodexCursor(val value: String)

@Serializable
@JvmInline
value class CodexTimestamp(val epochSeconds: Long)

@Serializable
@JvmInline
value class ThreadSource(val value: String)

@Serializable
@JvmInline
value class ThreadStartSource(val value: String) {
    companion object {
        val Startup = ThreadStartSource("startup")
        val Clear = ThreadStartSource("clear")
    }
}

@Serializable
@JvmInline
value class ThreadSourceKind(val value: String) {
    companion object {
        val Cli = ThreadSourceKind("cli")
        val VsCode = ThreadSourceKind("vscode")
        val Exec = ThreadSourceKind("exec")
        val AppServer = ThreadSourceKind("appServer")
        val SubAgent = ThreadSourceKind("subAgent")
        val SubAgentReview = ThreadSourceKind("subAgentReview")
        val SubAgentCompact = ThreadSourceKind("subAgentCompact")
        val SubAgentThreadSpawn = ThreadSourceKind("subAgentThreadSpawn")
        val SubAgentOther = ThreadSourceKind("subAgentOther")
        val Unknown = ThreadSourceKind("unknown")
    }
}

@Serializable
@JvmInline
value class ThreadSortKey(val value: String) {
    companion object {
        val CreatedAt = ThreadSortKey("created_at")
        val UpdatedAt = ThreadSortKey("updated_at")
        val RecencyAt = ThreadSortKey("recency_at")
    }
}

@Serializable(with = ThreadListCwdFilterSerializer::class)
sealed interface ThreadListCwdFilter {
    data class Single(val path: CodexHostPath) : ThreadListCwdFilter

    data class AnyOf(val paths: List<CodexHostPath>) : ThreadListCwdFilter
}

internal object ThreadListCwdFilterSerializer : KSerializer<ThreadListCwdFilter> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun serialize(encoder: Encoder, value: ThreadListCwdFilter) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("ThreadListCwdFilter requires JSON encoding")
        val element = when (value) {
            is ThreadListCwdFilter.Single -> JsonPrimitive(value.path.value)
            is ThreadListCwdFilter.AnyOf -> buildJsonArray {
                value.paths.forEach { add(JsonPrimitive(it.value)) }
            }
        }
        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): ThreadListCwdFilter {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("ThreadListCwdFilter requires JSON decoding")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> ThreadListCwdFilter.Single(CodexHostPath(element.content))
            is JsonArray -> ThreadListCwdFilter.AnyOf(
                element.map { CodexHostPath(it.jsonPrimitive.content) },
            )
            else -> throw SerializationException("ThreadListCwdFilter must be a string or array")
        }
    }
}

@Serializable
@JvmInline
value class ThreadStatusType(val value: String) {
    companion object {
        val NotLoaded = ThreadStatusType("notLoaded")
    }
}

@Serializable
@JvmInline
value class ThreadGoalStatus(val value: String) {
    companion object {
        val Active = ThreadGoalStatus("active")
        val Blocked = ThreadGoalStatus("blocked")
        val BudgetLimited = ThreadGoalStatus("budgetLimited")
        val UsageLimited = ThreadGoalStatus("usageLimited")
    }
}

@Serializable
data class Thread(
    val id: ThreadId,
    val preview: String? = null,
    val modelProvider: String? = null,
    val createdAt: CodexTimestamp? = null,
    val updatedAt: CodexTimestamp? = null,
    val gitInfo: ThreadGitInfo? = null,
)

@Serializable
data class ThreadGoal(
    val threadId: ThreadId,
    val objective: String,
    val status: ThreadGoalStatus,
    val tokenBudget: Long? = null,
    val tokensUsed: Long = 0,
    val timeUsedSeconds: Long = 0,
    val createdAt: CodexTimestamp? = null,
    val updatedAt: CodexTimestamp? = null,
)

@Serializable
data class ThreadGitInfo(
    val sha: String? = null,
    val branch: String? = null,
    val originUrl: String? = null,
)

sealed interface ThreadMetadataPatchValue {
    data object Unchanged : ThreadMetadataPatchValue
    data object Clear : ThreadMetadataPatchValue
    data class Set(val value: String) : ThreadMetadataPatchValue
}

@Serializable(with = ThreadGitInfoPatchSerializer::class)
data class ThreadGitInfoPatch(
    val sha: ThreadMetadataPatchValue = ThreadMetadataPatchValue.Unchanged,
    val branch: ThreadMetadataPatchValue = ThreadMetadataPatchValue.Unchanged,
    val originUrl: ThreadMetadataPatchValue = ThreadMetadataPatchValue.Unchanged,
)

object ThreadGitInfoPatchSerializer : KSerializer<ThreadGitInfoPatch> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ThreadGitInfoPatch") {
        element<String?>("sha", isOptional = true)
        element<String?>("branch", isOptional = true)
        element<String?>("originUrl", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: ThreadGitInfoPatch) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("ThreadGitInfoPatch requires JSON encoding")
        jsonEncoder.encodeJsonElement(
            buildJsonObject {
                putPatchValue("sha", value.sha)
                putPatchValue("branch", value.branch)
                putPatchValue("originUrl", value.originUrl)
            },
        )
    }

    override fun deserialize(decoder: Decoder): ThreadGitInfoPatch {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("ThreadGitInfoPatch requires JSON decoding")
        val element = jsonDecoder.decodeJsonElement()
        val jsonObject = element as? JsonObject
            ?: throw SerializationException("ThreadGitInfoPatch must be a JSON object")
        return ThreadGitInfoPatch(
            sha = jsonObject.decodePatchValue("sha"),
            branch = jsonObject.decodePatchValue("branch"),
            originUrl = jsonObject.decodePatchValue("originUrl"),
        )
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putPatchValue(
        key: String,
        value: ThreadMetadataPatchValue,
    ) {
        when (value) {
            ThreadMetadataPatchValue.Unchanged -> Unit
            ThreadMetadataPatchValue.Clear -> put(key, JsonNull)
            is ThreadMetadataPatchValue.Set -> put(key, value.value)
        }
    }

    private fun JsonObject.decodePatchValue(key: String): ThreadMetadataPatchValue {
        val value = this[key] ?: return ThreadMetadataPatchValue.Unchanged
        return when (value) {
            JsonNull -> ThreadMetadataPatchValue.Clear
            is JsonPrimitive -> {
                val text = value.contentOrNull
                    ?: throw SerializationException("ThreadGitInfoPatch.$key must be a string or null")
                ThreadMetadataPatchValue.Set(text)
            }
            else -> throw SerializationException("ThreadGitInfoPatch.$key must be a string or null")
        }
    }
}

@Serializable
data class ThreadList(
    val threads: List<Thread> = emptyList(),
    val nextCursor: CodexCursor? = null,
    val backwardsCursor: CodexCursor? = null,
)

@Serializable
data class ListLoadedThreadsRequest(
    val cursor: CodexCursor? = null,
    val limit: Int? = null,
) {
    init {
        require(limit == null || limit >= 0) { "limit must be non-negative" }
    }
}

@Serializable
data class LoadedThreadList(
    val threadIds: List<ThreadId>,
    val nextCursor: CodexCursor? = null,
)

@Serializable
data class StartThreadRequest(
    val serviceTier: ServiceTier? = null,
    val cwd: CodexHostPath? = null,
    val approvalPolicy: ApprovalPolicy? = null,
    val approvalsReviewer: ApprovalsReviewer? = null,
    val baseInstructions: String? = null,
    val config: CodexJsonPayload? = null,
    val developerInstructions: String? = null,
    val serviceName: String? = null,
    val sessionStartSource: ThreadStartSource? = null,
    val ephemeral: Boolean? = null,
    val sandbox: SandboxMode? = null,
    val threadSource: ThreadSource? = null,
    val model: ModelName? = null,
    val modelProvider: String? = null,
    val personality: Personality? = null,
)

@Serializable
data class ResumeThreadRequest(
    val threadId: ThreadId,
    val approvalPolicy: ApprovalPolicy? = null,
    val approvalsReviewer: ApprovalsReviewer? = null,
    val baseInstructions: String? = null,
    val config: CodexJsonPayload? = null,
    val cwd: CodexHostPath? = null,
    val developerInstructions: String? = null,
    val personality: Personality? = null,
    val sandbox: SandboxMode? = null,
    val model: ModelName? = null,
    val modelProvider: String? = null,
    val serviceTier: ServiceTier? = null,
)

@Serializable
data class ForkThreadRequest(
    val threadId: ThreadId,
    val approvalPolicy: ApprovalPolicy? = null,
    val approvalsReviewer: ApprovalsReviewer? = null,
    val baseInstructions: String? = null,
    val config: CodexJsonPayload? = null,
    val cwd: CodexHostPath? = null,
    val sandbox: SandboxMode? = null,
    val developerInstructions: String? = null,
    val ephemeral: Boolean? = null,
    val threadSource: ThreadSource? = null,
    val lastTurnId: TurnId? = null,
    val model: ModelName? = null,
    val modelProvider: String? = null,
    val serviceTier: ServiceTier? = null,
)

@Serializable
data class ListThreadsRequest(
    val sourceKinds: List<ThreadSourceKind>? = null,
    val archived: Boolean? = null,
    val cursor: CodexCursor? = null,
    val cwd: ThreadListCwdFilter? = null,
    val isPinned: Boolean? = null,
    val limit: Int? = null,
    val modelProviders: List<String>? = null,
    val useStateDbOnly: Boolean? = null,
    val searchTerm: String? = null,
    val sortDirection: SortDirection? = null,
    val sortKey: ThreadSortKey? = null,
)

@Serializable
data class ReadThreadRequest(
    val threadId: ThreadId,
    val includeTurns: Boolean? = null,
)

@Serializable
data class SetThreadNameRequest(
    val threadId: ThreadId,
    val name: String,
)
