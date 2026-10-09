package io.github.vupoint.cokit.client.approvals

/**
 * Application policy callback for command execution or stdin approval. Without a registered handler, CoKit declines the request.
 */
fun interface CommandApprovalHandler {
    /** Evaluates untrusted request context and returns an explicit decision; registration does not imply acceptance. */
    suspend fun decide(request: CommandApprovalRequest): ApprovalDecision
}
