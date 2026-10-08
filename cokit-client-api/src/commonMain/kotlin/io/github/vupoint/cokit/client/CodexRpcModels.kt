package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.tools.DynamicToolSpec

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire parameters for `thread/start`; [StartThreadRequest] is the high-level equivalent.
 *
 * Null overrides are omitted by the protocol JSON defaults. A non-null [dynamicTools] list
 * requires experimental initialization and Kotlin API opt-in.
 */
@OptIn(ExperimentalCodexApi::class)
@Serializable
data class ThreadStartParams(
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

/** Thread created by `thread/start`, together with effective server configuration. */
@Serializable
data class ThreadStartResult(
    val thread: Thread,
    val model: ModelName? = null,
    val modelProvider: String? = null,
    val cwd: CodexHostPath? = null,
    val approvalPolicy: ApprovalPolicy? = null,
    val approvalsReviewer: ApprovalsReviewer? = null,
    val sandbox: SandboxPolicy? = null,
    val reasoningEffort: ReasoningEffort? = null,
    val serviceTier: ServiceTier? = null,
    val disabledPluginIds: List<String> = emptyList(),
    val instructionSources: List<CodexHostPath> = emptyList(),
)

/**
 * Wire parameters for `thread/resume`; [ResumeThreadRequest] is the high-level equivalent.
 *
 * Null overrides are omitted. [excludeTurns] controls whether returned turn history is included.
 */
@Serializable
data class ThreadResumeParams(
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

/** Resumed thread and effective configuration, with cursors for available paginated history. */
@Serializable
data class ThreadResumeResult(
    val thread: Thread,
    val itemsBackwardsCursor: CodexCursor? = null,
    val turnsBackwardsCursor: CodexCursor? = null,
    val collaborationMode: ThreadCollaborationMode? = null,
    val model: ModelName? = null,
    val modelProvider: String? = null,
    val cwd: CodexHostPath? = null,
    val approvalPolicy: ApprovalPolicy? = null,
    val approvalsReviewer: ApprovalsReviewer? = null,
    val sandbox: SandboxPolicy? = null,
    val reasoningEffort: ReasoningEffort? = null,
    val serviceTier: ServiceTier? = null,
    val disabledPluginIds: List<String> = emptyList(),
    val instructionSources: List<CodexHostPath> = emptyList(),
)

/**
 * Wire parameters for `thread/fork`; [ForkThreadRequest] is the high-level equivalent.
 *
 * [threadId] selects the source conversation; null overrides and history-boundary options are omitted.
 */
@Serializable
data class ThreadForkParams(
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

/** New forked thread and its effective server configuration. */
@Serializable
data class ThreadForkResult(
    val thread: Thread,
    val model: ModelName? = null,
    val modelProvider: String? = null,
    val cwd: CodexHostPath? = null,
    val approvalPolicy: ApprovalPolicy? = null,
    val approvalsReviewer: ApprovalsReviewer? = null,
    val sandbox: SandboxPolicy? = null,
    val reasoningEffort: ReasoningEffort? = null,
    val serviceTier: ServiceTier? = null,
    val disabledPluginIds: List<String> = emptyList(),
    val instructionSources: List<CodexHostPath> = emptyList(),
)

/**
 * Wire filters and pagination for `thread/list`; [ListThreadsRequest] is the high-level equivalent.
 *
 * [sectionId] distinguishes an omitted filter from an explicit null selecting unsectioned
 * threads. Other null filters are omitted.
 */
@Serializable
data class ThreadListParams(
    val sourceKinds: List<ThreadSourceKind>? = null,
    val archived: Boolean? = null,
    val cursor: CodexCursor? = null,
    val cwd: ThreadListCwdFilter? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val sectionId: CodexOptional<ThreadSectionId> = CodexOptional.Omitted,
    val originators: List<String>? = null,
    val limit: Int? = null,
    val modelProviders: List<String>? = null,
    val useStateDbOnly: Boolean? = null,
    val searchTerm: String? = null,
    val sortDirection: SortDirection? = null,
    val sortKey: ThreadSortKey? = null,
)

/** Thread page from `thread/list`; [threads] is encoded under the wire key `data`. */
@Serializable
data class ThreadListResult(
    @SerialName("data")
    val threads: List<Thread> = emptyList(),
    val nextCursor: CodexCursor? = null,
    val backwardsCursor: CodexCursor? = null,
)

/** Wire pagination for `thread/loaded/list`, which lists currently loaded server threads. */
@Serializable
data class ThreadLoadedListParams(
    val cursor: CodexCursor? = null,
    val limit: Int? = null,
)

/** Loaded thread IDs from `thread/loaded/list`, encoded under the wire key `data`. */
@Serializable
data class ThreadLoadedListResult(
    @SerialName("data")
    val threadIds: List<ThreadId>,
    val nextCursor: CodexCursor? = null,
)

/** Wire parameters for `thread/read`, optionally requesting inline turn history. */
@Serializable
data class ThreadReadParams(
    val threadId: ThreadId,
    val includeTurns: Boolean? = null,
)

/** Thread snapshot returned by `thread/read`. */
@Serializable
data class ThreadReadResult(
    val thread: Thread,
)

/** Selects a thread for `thread/archive`. */
@Serializable
data class ThreadArchiveParams(
    val threadId: ThreadId,
)

/** Selects a thread for `thread/unarchive`. */
@Serializable
data class ThreadUnarchiveParams(
    val threadId: ThreadId,
)

/** Thread snapshot after `thread/unarchive`. */
@Serializable
data class ThreadUnarchiveResult(
    val thread: Thread,
)

/** Selects a thread for the `thread/delete` operation. */
@Serializable
data class ThreadDeleteParams(
    val threadId: ThreadId,
)

/** Selects a thread for `thread/unsubscribe`, ending its notification subscription. */
@Serializable
data class ThreadUnsubscribeParams(
    val threadId: ThreadId,
)

/** Wire name update for `thread/name/set`. */
@Serializable
data class ThreadSetNameParams(
    val threadId: ThreadId,
    val name: String,
)

/**
 * Wire metadata update for `thread/metadata/update`.
 *
 * A null [gitInfo] omits the patch; individual patch fields distinguish preserving and clearing.
 */
@Serializable
data class ThreadMetadataUpdateParams(
    val threadId: ThreadId,
    val gitInfo: ThreadGitInfoPatch? = null,
)

/** Updated thread snapshot from `thread/metadata/update`. */
@Serializable
data class ThreadMetadataUpdateResult(
    val thread: Thread,
)

/**
 * Sets or updates a thread goal through `thread/goal/set`.
 *
 * [tokenBudget] is a token count rather than a time limit. Null fields are omitted.
 */
@Serializable
data class ThreadGoalSetParams(
    val threadId: ThreadId,
    val objective: String? = null,
    val status: ThreadGoalStatus? = null,
    val tokenBudget: Long? = null,
)

/** Goal snapshot returned by `thread/goal/set`. */
@Serializable
data class ThreadGoalSetResult(
    val goal: ThreadGoal,
)

/** Selects a thread for `thread/goal/get`. */
@Serializable
data class ThreadGoalGetParams(
    val threadId: ThreadId,
)

/** Goal lookup result; null [goal] means the server supplied no goal. */
@Serializable
data class ThreadGoalGetResult(
    val goal: ThreadGoal? = null,
)

/** Selects a thread whose goal should be cleared by `thread/goal/clear`. */
@Serializable
data class ThreadGoalClearParams(
    val threadId: ThreadId,
)

/** Reports whether `thread/goal/clear` cleared a goal. */
@Serializable
data class ThreadGoalClearResult(
    val cleared: Boolean,
)

/** Selects a thread for the `thread/compact/start` operation. */
@Serializable
data class ThreadCompactionStartParams(
    val threadId: ThreadId,
)

/** Wire pagination and item-detail selection for `thread/turns/list`. */
@Serializable
data class ThreadTurnsListParams(
    val threadId: ThreadId,
    val cursor: CodexCursor? = null,
    val limit: Int? = null,
    val sortDirection: SortDirection? = null,
    val itemsView: TurnItemsView? = null,
)

/** Page of turns with opaque continuation tokens for either direction. */
@Serializable
data class ThreadTurnsListResult(
    val data: List<Turn> = emptyList(),
    val nextCursor: CodexCursor? = null,
    val backwardsCursor: CodexCursor? = null,
)

/**
 * Wire parameters for `turn/start`; [StartTurnRequest] is the high-level equivalent.
 *
 * The [input] list is always encoded, including when empty. Null overrides are omitted.
 * [sandbox] uses the wire key `sandboxPolicy`.
 */
@Serializable
data class TurnStartParams(
    val threadId: ThreadId,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val input: List<TurnInput> = emptyList(),
    val cwd: CodexHostPath? = null,
    val approvalPolicy: ApprovalPolicy? = null,
    val approvalsReviewer: ApprovalsReviewer? = null,
    val clientUserMessageId: ClientMessageId? = null,
    val serviceTier: ServiceTier? = null,
    /** Applies only to a newly started turn and does not change the thread tier. */
    val serviceTierForTurn: ServiceTier? = null,
    val toolOutput: TurnToolOutput? = null,
    val turnTrigger: String? = null,
    /** Saved selection only; upstream does not yet filter plugin capabilities. */
    val disabledPluginIds: List<String>? = null,
    @SerialName("sandboxPolicy")
    val sandbox: SandboxPolicy? = null,
    val model: ModelName? = null,
    val effort: ReasoningEffort? = null,
    val summary: ReasoningSummary? = null,
    val outputSchema: CodexJsonPayload? = null,
    val personality: Personality? = null,
)

/** Initial turn snapshot from `turn/start`; notifications report later progress and completion. */
@Serializable
data class TurnStartResult(
    val turn: Turn,
)

/** Wire input for `turn/steer`, guarded by the caller's [expectedTurnId]. */
@Serializable
data class TurnSteerParams(
    val threadId: ThreadId,
    val expectedTurnId: TurnId,
    val input: List<TurnInput>,
    val clientUserMessageId: ClientMessageId? = null,
)

/** Identifier of the turn that accepted `turn/steer` input. */
@Serializable
data class TurnSteerResult(
    val turnId: TurnId,
)

/** Selects a thread and turn for `turn/interrupt`; acknowledgement is distinct from completion. */
@Serializable
data class TurnInterruptParams(
    val threadId: ThreadId,
    val turnId: TurnId,
)

/** Empty JSON object result used by RPC operations that return no structured data. */
@Serializable
data object CodexRpcUnit
