package io.github.vupoint.cokit.client

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

/** Opaque identifier for one turn of work within a conversation. */
@Serializable
@JvmInline
value class TurnId(val value: String)

/** Caller-supplied message identity used to correlate submitted user input with server events. */
@Serializable
@JvmInline
value class ClientMessageId(val value: String)

/** Server list ordering direction; unknown strings remain representable. */
@Serializable
@JvmInline
value class SortDirection(val value: String) {
    companion object {
        val Asc = SortDirection("asc")
        val Desc = SortDirection("desc")
    }
}

/** Item detail level reported or requested for turn history. */
@Serializable
@JvmInline
value class TurnItemsView(val value: String) {
    companion object {
        val NotLoaded = TurnItemsView("notLoaded")
        val Summary = TurnItemsView("summary")
        val Full = TurnItemsView("full")
    }
}

/** Turn lifecycle state. A terminal state can be completed, interrupted, or failed. */
@Serializable
@JvmInline
value class TurnStatus(val value: String) {
    companion object {
        val InProgress = TurnStatus("inProgress")
        val Completed = TurnStatus("completed")
        val Interrupted = TurnStatus("interrupted")
        val Failed = TurnStatus("failed")
    }
}

/**
 * Snapshot of a turn and its available items.
 *
 * [items] preserves raw item JSON, including fields not modeled by [ThreadItemSummary].
 * An empty list may reflect [itemsView], rather than the absence of work.
 *
 * @property startedAt Optional start timestamp in Unix seconds.
 * @property completedAt Optional completion timestamp in Unix seconds.
 * @property durationMs Optional elapsed duration in milliseconds.
 */
@Serializable
data class Turn(
    val id: TurnId,
    val status: TurnStatus,
    val itemsView: TurnItemsView? = null,
    val items: List<CodexJsonPayload> = emptyList(),
    /** Errors may accompany failed or interrupted turns, including approval denial limits. */
    val error: TurnError? = null,
    val startedAt: CodexTimestamp? = null,
    val completedAt: CodexTimestamp? = null,
    val durationMs: Long? = null,
)

/**
 * Failure or interruption details attached to a turn.
 *
 * [codexErrorInfo] preserves structured server error data without narrowing its variants.
 */
@Serializable
data class TurnError(
    val message: String,
    val codexErrorInfo: CodexJsonPayload? = null,
    val additionalDetails: String? = null,
)

/**
 * Submits input or tool output through [TurnsApi.start].
 *
 * The empty [input] list is sent explicitly. Null configuration overrides are omitted;
 * start returns a turn snapshot, while notifications report subsequent progress and completion.
 */
@Serializable
data class StartTurnRequest(
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
    val sandboxPolicy: SandboxPolicy? = null,
    val model: ModelName? = null,
    val effort: ReasoningEffort? = null,
    val summary: ReasoningSummary? = null,
    val outputSchema: CodexJsonPayload? = null,
    val personality: Personality? = null,
)

/**
 * Adds input to an active turn through [TurnsApi.steer].
 *
 * [expectedTurnId] identifies the turn the caller expects to be active so the server
 * can reject steering that would target a different turn.
 */
@Serializable
data class SteerTurnRequest(
    val threadId: ThreadId,
    val expectedTurnId: TurnId,
    val input: List<TurnInput>,
    val clientUserMessageId: ClientMessageId? = null,
)

/**
 * Requests interruption of the identified turn through [TurnsApi.interrupt].
 *
 * The response acknowledges the request; terminal state is reported through turn notifications.
 */
@Serializable
data class InterruptTurnRequest(
    val threadId: ThreadId,
    val turnId: TurnId,
)
