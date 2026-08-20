package io.github.vupoint.cokit.client.approvals

fun interface CommandApprovalHandler {
    suspend fun decide(request: CommandApprovalRequest): ApprovalDecision
}
