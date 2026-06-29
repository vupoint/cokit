package io.github.vupoint.cokit.client

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ThreadStartParams(
    val cwd: CodexHostPath? = null,
    val approvalPolicy: ApprovalPolicy? = null,
    val sandbox: SandboxPolicy? = null,
    val permissions: CodexJsonPayload? = null,
    val model: ModelName? = null,
    val effort: ReasoningEffort? = null,
    val personality: String? = null,
)

@Serializable
data class ThreadStartResult(
    val thread: Thread,
)

@Serializable
data class ThreadResumeParams(
    val threadId: ThreadId,
    val excludeTurns: List<TurnId> = emptyList(),
    val initialTurnsPage: CodexJsonPayload? = null,
)

@Serializable
data class ThreadResumeResult(
    val thread: Thread,
)

@Serializable
data class ThreadForkParams(
    val threadId: ThreadId,
    val ephemeral: Boolean? = null,
    val excludeTurns: List<TurnId> = emptyList(),
)

@Serializable
data class ThreadForkResult(
    val thread: Thread,
)

@Serializable
data class ThreadListParams(
    val cursor: CodexCursor? = null,
    val limit: Int? = null,
    val cwd: CodexHostPath? = null,
    val archived: Boolean? = null,
    val searchTerm: String? = null,
)

@Serializable
data class ThreadListResult(
    val threads: List<Thread> = emptyList(),
    val cursor: CodexCursor? = null,
)

@Serializable
data class ThreadReadParams(
    val threadId: ThreadId,
    val includeTurns: Boolean? = null,
)

@Serializable
data class ThreadReadResult(
    val thread: Thread,
)

@Serializable
data class ThreadArchiveParams(
    val threadId: ThreadId,
)

@Serializable
data class ThreadUnarchiveParams(
    val threadId: ThreadId,
)

@Serializable
data class ThreadDeleteParams(
    val threadId: ThreadId,
)

@Serializable
data class ThreadUnsubscribeParams(
    val threadId: ThreadId,
)

@Serializable
data class ThreadSetNameParams(
    val threadId: ThreadId,
    val name: String,
)

@Serializable
data class ThreadMetadataUpdateParams(
    val threadId: ThreadId,
    val gitInfo: ThreadGitInfoPatch? = null,
)

@Serializable
data class ThreadMetadataUpdateResult(
    val thread: Thread,
)

@Serializable
data class ThreadGoalSetParams(
    val threadId: ThreadId,
    val objective: String? = null,
    val status: ThreadGoalStatus? = null,
    val tokenBudget: Long? = null,
)

@Serializable
data class ThreadGoalSetResult(
    val goal: ThreadGoal,
)

@Serializable
data class ThreadGoalGetParams(
    val threadId: ThreadId,
)

@Serializable
data class ThreadGoalGetResult(
    val goal: ThreadGoal? = null,
)

@Serializable
data class ThreadGoalClearParams(
    val threadId: ThreadId,
)

@Serializable
data class ThreadGoalClearResult(
    val cleared: Boolean,
)

@Serializable
data class ThreadCompactionStartParams(
    val threadId: ThreadId,
)

@ExperimentalCodexApi
@Serializable
data class ThreadTurnsListParams(
    val threadId: ThreadId,
    val cursor: CodexCursor? = null,
    val limit: Int? = null,
    val sortDirection: SortDirection? = null,
    val itemsView: TurnItemsView? = null,
)

@ExperimentalCodexApi
@Serializable
data class ThreadTurnsListResult(
    val data: List<Turn> = emptyList(),
    val nextCursor: CodexCursor? = null,
    val backwardsCursor: CodexCursor? = null,
)

@Serializable
data class TurnStartParams(
    val threadId: ThreadId,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val input: List<TurnInput> = emptyList(),
    val cwd: CodexHostPath? = null,
    val approvalPolicy: ApprovalPolicy? = null,
    @SerialName("sandboxPolicy")
    val sandbox: SandboxPolicy? = null,
    val permissions: CodexJsonPayload? = null,
    val model: ModelName? = null,
    val effort: ReasoningEffort? = null,
    val outputSchema: CodexJsonPayload? = null,
)

@Serializable
data class TurnStartResult(
    val turn: Turn,
)

@Serializable
data class TurnSteerParams(
    val threadId: ThreadId,
    val expectedTurnId: TurnId,
    val input: List<TurnInput>,
    val clientUserMessageId: ClientMessageId? = null,
)

@Serializable
data class TurnInterruptParams(
    val threadId: ThreadId,
    val turnId: TurnId,
)

@Serializable
data object CodexRpcUnit
