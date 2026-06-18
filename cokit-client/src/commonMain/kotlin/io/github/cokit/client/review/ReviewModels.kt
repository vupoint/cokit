package io.github.cokit.client.review

import io.github.cokit.client.ThreadId
import io.github.cokit.client.Turn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ReviewDelivery(val value: String) {
    companion object {
        val Inline = ReviewDelivery("inline")
        val Detached = ReviewDelivery("detached")
    }
}

@Serializable
sealed interface ReviewTarget {
    @Serializable
    @SerialName("uncommittedChanges")
    data object UncommittedChanges : ReviewTarget

    @Serializable
    @SerialName("baseBranch")
    data class BaseBranch(
        val branch: String,
    ) : ReviewTarget

    @Serializable
    @SerialName("commit")
    data class Commit(
        val sha: String,
        val title: String? = null,
    ) : ReviewTarget

    @Serializable
    @SerialName("custom")
    data class Custom(
        val instructions: String,
    ) : ReviewTarget
}

@Serializable
data class ReviewStartParams(
    val threadId: ThreadId,
    val target: ReviewTarget,
    val delivery: ReviewDelivery? = null,
)

@Serializable
data class ReviewStartResult(
    val reviewThreadId: ThreadId,
    val turn: Turn,
)
