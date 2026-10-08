package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.tools.DynamicToolSpec

import kotlinx.serialization.EncodeDefault
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

/** Opaque app-server conversation identifier; unrelated to JSON-RPC request IDs. */
@Serializable
@JvmInline
value class ThreadId(val value: String)

/** Opaque server pagination token. Reuse it unchanged with the corresponding list operation. */
@Serializable
@JvmInline
value class CodexCursor(val value: String)

/** Unix timestamp in whole seconds since the UTC epoch, not milliseconds. */
@Serializable
@JvmInline
value class CodexTimestamp(val epochSeconds: Long)

/** Separate string-valued thread provenance; [Thread.source] preserves the richer source union. */
@Serializable
@JvmInline
value class ThreadSource(val value: String)

/** Session-start trigger supplied when creating a thread; known triggers are companion values. */
@Serializable
@JvmInline
value class ThreadStartSource(val value: String) {
    companion object {
        val Startup = ThreadStartSource("startup")
        val Clear = ThreadStartSource("clear")
    }
}

/** Source category used by thread-list filters, including subagent categories. */
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

/** Server thread ordering key; arbitrary strings preserve future protocol values. */
@Serializable
@JvmInline
value class ThreadSortKey(val value: String) {
    companion object {
        val CreatedAt = ThreadSortKey("created_at")
        val UpdatedAt = ThreadSortKey("updated_at")
        val RecencyAt = ThreadSortKey("recency_at")
        val SectionPosition = ThreadSortKey("section_position")
    }
}

/** Matches a single server working directory or any directory in a supplied list. */
@Serializable(with = ThreadListCwdFilterSerializer::class)
sealed interface ThreadListCwdFilter {
    /** Filters by one working-directory path on the app-server host. */
    data class Single(val path: CodexHostPath) : ThreadListCwdFilter

    /** Filters by any of the supplied working-directory paths on the app-server host. */
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

/** Thread runtime state, distinct from the status of an individual turn. */
@Serializable
@JvmInline
value class ThreadStatusType(val value: String) {
    companion object {
        val NotLoaded = ThreadStatusType("notLoaded")
        val Idle = ThreadStatusType("idle")
        val Active = ThreadStatusType("active")
        val SystemError = ThreadStatusType("systemError")
    }
}

/** Goal lifecycle state, including server-imposed token-budget and usage limits. */
@Serializable
@JvmInline
value class ThreadGoalStatus(val value: String) {
    companion object {
        val Active = ThreadGoalStatus("active")
        val Paused = ThreadGoalStatus("paused")
        val Complete = ThreadGoalStatus("complete")
        val Blocked = ThreadGoalStatus("blocked")
        val BudgetLimited = ThreadGoalStatus("budgetLimited")
        val UsageLimited = ThreadGoalStatus("usageLimited")
    }
}

/**
 * Server thread snapshot with metadata and any included turn history.
 *
 * Nullable fields represent metadata unavailable in this response. An empty [turns] list does not
 * prove the thread has no history: the request or history mode may exclude turns.
 *
 * @property forkedFromId Source conversation when this thread was forked.
 * @property parentThreadId Parent conversation when this thread belongs to a parent.
 * @property createdAt Creation time in Unix seconds.
 * @property updatedAt Last update time in Unix seconds.
 */
@Serializable
data class Thread(
    val id: ThreadId,
    val preview: String? = null,
    val modelProvider: String? = null,
    val createdAt: CodexTimestamp? = null,
    val updatedAt: CodexTimestamp? = null,
    val gitInfo: ThreadGitInfo? = null,
    val name: String? = null,
    val status: ThreadStatus? = null,
    val historyMode: ThreadHistoryMode? = null,
    val model: ModelName? = null,
    val reasoningEffort: ReasoningEffort? = null,
    val originator: String? = null,
    val sessionId: String? = null,
    val forkedFromId: ThreadId? = null,
    val parentThreadId: ThreadId? = null,
    val projectId: String? = null,
    val section: ThreadSection? = null,
    val sectionEnteredAt: CodexTimestamp? = null,
    val recencyAt: CodexTimestamp? = null,
    val cwd: CodexHostPath? = null,
    val ephemeral: Boolean? = null,
    val cliVersion: String? = null,
    val agentNickname: String? = null,
    val agentRole: String? = null,
    /** Preserves the string/custom/subagent source union without losing future variants. */
    val source: CodexJsonPayload? = null,
    val threadSource: ThreadSource? = null,
    val turns: List<Turn> = emptyList(),
)

/**
 * Objective and usage accounting for a thread goal.
 *
 * @property tokenBudget Optional token cap; null indicates no cap was reported.
 * @property tokensUsed Number of tokens charged to the goal.
 * @property timeUsedSeconds Time charged to the goal in seconds.
 */
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

/** Optional Git revision, branch, and remote metadata reported for a thread. */
@Serializable
data class ThreadGitInfo(
    val sha: String? = null,
    val branch: String? = null,
    val originUrl: String? = null,
)

/** Distinguishes preserving, clearing, and replacing a Git metadata field. */
sealed interface ThreadMetadataPatchValue {
    /** Omits the patch field, preserving its existing server value. */
    data object Unchanged : ThreadMetadataPatchValue
    /** Sends JSON null to clear the existing metadata field. */
    data object Clear : ThreadMetadataPatchValue
    /** Replaces the metadata field with [value]. */
    data class Set(val value: String) : ThreadMetadataPatchValue
}

/** Partial Git metadata update. Each field defaults to leaving its server value unchanged. */
@Serializable(with = ThreadGitInfoPatchSerializer::class)
data class ThreadGitInfoPatch(
    val sha: ThreadMetadataPatchValue = ThreadMetadataPatchValue.Unchanged,
    val branch: ThreadMetadataPatchValue = ThreadMetadataPatchValue.Unchanged,
    val originUrl: ThreadMetadataPatchValue = ThreadMetadataPatchValue.Unchanged,
)

/** JSON serializer preserving absent, null, and string-valued metadata patch fields. */
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

/**
 * Page returned by [ThreadsApi.list].
 *
 * Reuse non-null cursors unchanged for continuation in the corresponding direction.
 */
@Serializable
data class ThreadList(
    val threads: List<Thread> = emptyList(),
    val nextCursor: CodexCursor? = null,
    val backwardsCursor: CodexCursor? = null,
)

/**
 * Pagination for threads currently loaded in the server runtime, rather than all stored threads.
 *
 * [limit] must be non-negative when supplied; null uses the server default.
 */
@Serializable
data class ListLoadedThreadsRequest(
    val cursor: CodexCursor? = null,
    val limit: Int? = null,
) {
    init {
        require(limit == null || limit >= 0) { "limit must be non-negative" }
    }
}

/** Page of runtime-loaded thread identifiers; null [nextCursor] means no continuation was supplied. */
@Serializable
data class LoadedThreadList(
    val threadIds: List<ThreadId>,
    val nextCursor: CodexCursor? = null,
)

/**
 * Configuration for [ThreadsApi.start]. Null overrides are omitted and use server defaults.
 *
 * Setting [dynamicTools], including an empty list, requires experimental initialization and API opt-in.
 */
@OptIn(ExperimentalCodexApi::class)
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
    /** Experimental tools for this thread; null omits the field, while an empty list is explicit. */
    @ExperimentalCodexApi
    val dynamicTools: List<DynamicToolSpec>? = null,
)

/**
 * Loads an existing thread for [ThreadsApi.resume], with optional configuration overrides.
 *
 * Null overrides are omitted. [excludeTurns] controls whether the response includes turn history.
 */
@Serializable
data class ResumeThreadRequest(
    val threadId: ThreadId,
    val excludeTurns: Boolean? = null,
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

/**
 * Creates a new thread from [threadId] through [ThreadsApi.fork].
 *
 * Null configuration overrides are omitted. [lastTurnId] optionally selects the history boundary;
 * [excludeTurns] controls returned turn history.
 */
@Serializable
data class ForkThreadRequest(
    val threadId: ThreadId,
    val excludeTurns: Boolean? = null,
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

/**
 * Filters and pagination for [ThreadsApi.list]. Null filters are omitted.
 *
 * [sectionId] is intentionally tri-state: omission leaves the filter unset,
 * `CodexOptional.Value(null)` selects threads without a section, and a value selects that section.
 */
@Serializable
data class ListThreadsRequest(
    val sourceKinds: List<ThreadSourceKind>? = null,
    val archived: Boolean? = null,
    val cursor: CodexCursor? = null,
    val cwd: ThreadListCwdFilter? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val sectionId: CodexOptional<ThreadSectionId> = CodexOptional.Omitted,
    /** Nonempty originator filters require a hosted backend. */
    val originators: List<String>? = null,
    val limit: Int? = null,
    val modelProviders: List<String>? = null,
    val useStateDbOnly: Boolean? = null,
    val searchTerm: String? = null,
    val sortDirection: SortDirection? = null,
    val sortKey: ThreadSortKey? = null,
)

/** Reads stored thread metadata through [ThreadsApi.read]; [includeTurns] optionally requests history. */
@Serializable
data class ReadThreadRequest(
    val threadId: ThreadId,
    val includeTurns: Boolean? = null,
)

/** Sets the display name of an existing thread through [ThreadsApi.setName]. */
@Serializable
data class SetThreadNameRequest(
    val threadId: ThreadId,
    val name: String,
)
