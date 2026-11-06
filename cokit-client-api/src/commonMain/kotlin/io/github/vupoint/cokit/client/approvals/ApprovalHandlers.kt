package io.github.vupoint.cokit.client.approvals

/**
 * Application callback for command execution or stdin approval. Human checkpoints require separate
 * registration and a fresh human decision. Without the corresponding handler, CoKit declines.
 */
fun interface CommandApprovalHandler {
    /** Evaluates untrusted request context and returns an explicit decision; registration does not imply acceptance. */
    suspend fun decide(request: CommandApprovalRequest): ApprovalDecision
}
