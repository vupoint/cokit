package io.github.vupoint.cokit.client

import kotlinx.serialization.Serializable

@Serializable
data class ThreadItemsListParams(
    val threadId: ThreadId,
    val turnId: TurnId? = null,
    val cursor: CodexCursor? = null,
    val limit: Int? = null,
    val sortDirection: SortDirection? = null,
)

@Serializable
data class ThreadItemEntry(
    val turnId: TurnId,
    val item: CodexJsonPayload,
    val startedAtMs: Long? = null,
    val completedAtMs: Long? = null,
)

@Serializable
data class ThreadItemsListResult(
    val data: List<ThreadItemEntry>,
    val nextCursor: CodexCursor? = null,
    val backwardsCursor: CodexCursor? = null,
)

/** Replaces paginated conversation history before a turn; does not revert filesystem changes. */
@Serializable
data class ThreadRevertParams(val threadId: ThreadId, val beforeTurnId: TurnId)

@Serializable
data class ThreadRevertResult(
    val thread: Thread,
    val itemsBackwardsCursor: CodexCursor? = null,
    val turnsBackwardsCursor: CodexCursor? = null,
)

@Serializable
@JvmInline
value class ThreadHistoryMode(val value: String) {
    companion object {
        val Legacy = ThreadHistoryMode("legacy")
        val Paginated = ThreadHistoryMode("paginated")
    }
}

@Serializable
data class ThreadStatus(val type: ThreadStatusType, val activeFlags: List<ThreadActiveFlag>? = null)

@Serializable
@JvmInline
value class ThreadActiveFlag(val value: String) {
    companion object {
        val WaitingOnApproval = ThreadActiveFlag("waitingOnApproval")
        val WaitingOnUserInput = ThreadActiveFlag("waitingOnUserInput")
    }
}

@Serializable
data class ThreadCollaborationMode(
    val mode: io.github.vupoint.cokit.client.environment.CollaborationModeKind,
    val settings: ThreadCollaborationSettings,
)

@Serializable
data class ThreadCollaborationSettings(
    val model: ModelName,
    @kotlinx.serialization.SerialName("reasoning_effort") val reasoningEffort: ReasoningEffort? = null,
    @kotlinx.serialization.SerialName("developer_instructions") val developerInstructions: String? = null,
)
