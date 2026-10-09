@file:OptIn(ExperimentalCodexApi::class)

package io.github.vupoint.cokit.client

import kotlinx.serialization.Serializable

/**
 * Experimental input waiting in a thread queue.
 *
 * [id] is the server queue-entry ID; [clientUserMessageId] is the caller message identity.
 * Queue APIs require experimental initialization and Kotlin opt-in.
 */
@ExperimentalCodexApi
@Serializable
data class QueuedSubmission(
    val id: String,
    val clientUserMessageId: String,
    val input: List<TurnInput>,
)

/** Adds input to the experimental queue with a caller-supplied message identity. */
@ExperimentalCodexApi
@Serializable
data class ThreadQueueAddParams(
    val threadId: ThreadId,
    val clientUserMessageId: String,
    val input: List<TurnInput>,
)

/** Pagination for a thread's experimental submission queue. */
@ExperimentalCodexApi
@Serializable
data class ThreadQueueListParams(
    val threadId: ThreadId,
    val cursor: CodexCursor? = null,
    val limit: Int? = null,
)

/** Page of queued submissions with an optional continuation token. */
@ExperimentalCodexApi
@Serializable
data class ThreadQueueListResult(
    val data: List<QueuedSubmission>,
    val nextCursor: CodexCursor? = null,
)

/** Replaces the input of the queued entry identified by [queuedSubmissionId]. */
@ExperimentalCodexApi
@Serializable
data class ThreadQueueUpdateParams(
    val threadId: ThreadId,
    val queuedSubmissionId: String,
    val input: List<TurnInput>,
)

/** Queued entry returned by an experimental add or update operation. */
@ExperimentalCodexApi
@Serializable
data class ThreadQueueSubmissionResult(
    val queuedSubmission: QueuedSubmission,
)

/** Identifies one queued entry for deletion from its thread queue. */
@ExperimentalCodexApi
@Serializable
data class ThreadQueueDeleteParams(
    val threadId: ThreadId,
    val queuedSubmissionId: String,
)

/** Reports whether the requested queued entry was deleted. */
@ExperimentalCodexApi
@Serializable
data class ThreadQueueDeleteResult(
    val deleted: Boolean,
)

/** Supplies the desired queue ordering as server queue-entry IDs. */
@ExperimentalCodexApi
@Serializable
data class ThreadQueueReorderParams(
    val threadId: ThreadId,
    val queuedSubmissionIds: List<String>,
)

/**
 * Starts a turn from the experimental queue.
 *
 * A null [queuedSubmissionId] omits an explicit entry selection and leaves selection to the server.
 */
@ExperimentalCodexApi
@Serializable
data class ThreadQueueStartParams(
    val threadId: ThreadId,
    val queuedSubmissionId: String? = null,
)

/** Initial turn snapshot after starting a queued submission; notifications report completion. */
@ExperimentalCodexApi
@Serializable
data class ThreadQueueStartResult(
    val turn: Turn,
)

