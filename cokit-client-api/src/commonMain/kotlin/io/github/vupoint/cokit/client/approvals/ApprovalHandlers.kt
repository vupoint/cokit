package io.github.vupoint.cokit.client.approvals

import io.github.vupoint.cokit.client.CodexHostPath
import io.github.vupoint.cokit.client.ThreadId
import io.github.vupoint.cokit.client.TurnId
import kotlinx.serialization.Serializable

@Serializable
data class CommandApprovalRequest(
    val threadId: ThreadId,
    val turnId: TurnId,
    val itemId: String,
    val command: String,
    val cwd: CodexHostPath? = null,
)

fun interface CommandApprovalHandler {
    suspend fun decide(request: CommandApprovalRequest): ApprovalDecision
}
