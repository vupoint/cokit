package io.github.vupoint.cokit.client.review

import io.github.vupoint.cokit.client.ThreadId
import io.github.vupoint.cokit.client.Turn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Server review placement. Codex 0.167.0 accepts only inline; unknown strings remain readable. */
@Serializable
@JvmInline
value class ReviewDelivery(val value: String) {
    companion object {
        val Inline = ReviewDelivery("inline")
        @Deprecated("Removed in Codex 0.167.0; start a separate thread, then review it inline")
        val Detached = ReviewDelivery("detached")
    }
}

/** Repository state or custom instructions to review on the app-server host. */
@Serializable
sealed interface ReviewTarget {
    /** Reviews uncommitted repository changes on the app-server host. */
    @Serializable
    @SerialName("uncommittedChanges")
    data object UncommittedChanges : ReviewTarget

    /** Reviews changes relative to the branch selected on the app-server host. */
    @Serializable
    @SerialName("baseBranch")
    data class BaseBranch(
        val branch: String,
    ) : ReviewTarget

    /** Reviews a selected commit, optionally using a caller-supplied display title. */
    @Serializable
    @SerialName("commit")
    data class Commit(
        val sha: String,
        val title: String? = null,
    ) : ReviewTarget

    /** Uses caller-supplied review instructions evaluated by app-server. */
    @Serializable
    @SerialName("custom")
    data class Custom(
        val instructions: String,
    ) : ReviewTarget
}

/** Starts a review on the supplied thread; omit [delivery] or select inline on Codex 0.167.0. */
@Serializable
data class ReviewStartParams(
    val threadId: ThreadId,
    val target: ReviewTarget,
    val delivery: ReviewDelivery? = null,
)

/** Review thread identity and started turn; this response starts review work rather than proving successful completion. */
@Serializable
data class ReviewStartResult(
    val reviewThreadId: ThreadId,
    val turn: Turn,
)
