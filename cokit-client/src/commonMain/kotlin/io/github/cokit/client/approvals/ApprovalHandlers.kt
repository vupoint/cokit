package io.github.cokit.client.approvals

import kotlinx.serialization.Serializable

@Serializable
data class CommandApprovalRequest(
    val threadId: String,
    val turnId: String,
    val itemId: String,
    val command: String,
    val cwd: String? = null,
)

fun interface CommandApprovalHandler {
    suspend fun decide(request: CommandApprovalRequest): ApprovalDecision
}
