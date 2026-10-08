package io.github.vupoint.cokit.client.approvals

import kotlinx.serialization.Serializable

/** Explicit outcome sent to app-server for command or file-change approval; CoKit defaults to [Decline]. */
@Serializable
sealed interface ApprovalDecision {
    /** Approves this requested action. */
    @Serializable
    data object Accept : ApprovalDecision

    /** Approves the action with session-scoped acceptance requested from app-server. */
    @Serializable
    data object AcceptForSession : ApprovalDecision

    /** Refuses this action; used when no application handler is registered. */
    @Serializable
    data object Decline : ApprovalDecision

    /** Cancels the approval flow rather than approving the action. */
    @Serializable
    data object Cancel : ApprovalDecision
}
