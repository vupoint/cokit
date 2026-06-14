package io.github.cokit.client

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ThreadStartParams(
    val cwd: CodexHostPath? = null,
    val approvalPolicy: ApprovalPolicy? = null,
    val sandbox: SandboxPolicy? = null,
    val model: ModelName? = null,
    val effort: ReasoningEffort? = null,
    val personality: String? = null,
)

@Serializable
data class ThreadStartResult(
    val thread: Thread,
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
    val model: ModelName? = null,
    val effort: ReasoningEffort? = null,
)

@Serializable
data class TurnStartResult(
    val turn: Turn,
)
