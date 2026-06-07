package io.github.cokit.client

import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.json.JsonElement

@Serializable
@JvmInline
value class TurnId(val value: String)

@Serializable
data class Turn(
    val id: TurnId,
    val status: String,
    val items: List<JsonElement> = emptyList(),
    val error: JsonElement? = null,
)

@Serializable
data class StartTurnRequest(
    val threadId: ThreadId,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val input: List<JsonElement> = emptyList(),
    val cwd: CodexHostPath? = null,
    val approvalPolicy: ApprovalPolicy? = null,
    val sandboxPolicy: SandboxPolicy? = null,
    val permissions: JsonElement? = null,
    val model: ModelName? = null,
    val effort: ReasoningEffort? = null,
    val outputSchema: JsonElement? = null,
)

@Serializable
data class SteerTurnRequest(
    val threadId: ThreadId,
    val expectedTurnId: TurnId,
    val input: List<JsonElement>,
    val clientUserMessageId: String? = null,
)

@Serializable
data class InterruptTurnRequest(
    val threadId: ThreadId,
    val turnId: TurnId,
)

@Serializable
internal data class TurnResult(
    val turn: Turn,
)
