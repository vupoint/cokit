package io.github.cokit.client.approvals

import kotlinx.serialization.Serializable

@Serializable
sealed interface ApprovalDecision {
    @Serializable
    data object Accept : ApprovalDecision

    @Serializable
    data object AcceptForSession : ApprovalDecision

    @Serializable
    data object Decline : ApprovalDecision

    @Serializable
    data object Cancel : ApprovalDecision
}
