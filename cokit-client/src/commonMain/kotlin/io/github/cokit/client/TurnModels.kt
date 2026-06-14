package io.github.cokit.client

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class TurnId(val value: String)

@Serializable
@JvmInline
value class ClientMessageId(val value: String)

@Serializable
@JvmInline
value class SortDirection(val value: String) {
    companion object {
        val Asc = SortDirection("asc")
        val Desc = SortDirection("desc")
    }
}

@Serializable
@JvmInline
value class TurnItemsView(val value: String) {
    companion object {
        val NotLoaded = TurnItemsView("notLoaded")
        val Summary = TurnItemsView("summary")
        val Full = TurnItemsView("full")
    }
}

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

@Serializable
data class Turn(
    val id: TurnId,
    val status: TurnStatus,
    val itemsView: TurnItemsView? = null,
    val items: List<CodexJsonPayload> = emptyList(),
    val error: CodexJsonPayload? = null,
)

@Serializable
data class StartTurnRequest(
    val threadId: ThreadId,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val input: List<TurnInput> = emptyList(),
    val cwd: CodexHostPath? = null,
    val approvalPolicy: ApprovalPolicy? = null,
    val sandboxPolicy: SandboxPolicy? = null,
    val permissions: CodexJsonPayload? = null,
    val model: ModelName? = null,
    val effort: ReasoningEffort? = null,
    val outputSchema: CodexJsonPayload? = null,
)

@Serializable
data class SteerTurnRequest(
    val threadId: ThreadId,
    val expectedTurnId: TurnId,
    val input: List<TurnInput>,
    val clientUserMessageId: ClientMessageId? = null,
)

@Serializable
data class InterruptTurnRequest(
    val threadId: ThreadId,
    val turnId: TurnId,
)
