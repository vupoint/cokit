@file:OptIn(ExperimentalCodexApi::class)

package io.github.vupoint.cokit.client

import kotlinx.serialization.Serializable

@ExperimentalCodexApi
@Serializable
data class QueuedSubmission(
    val id: String,
    val clientUserMessageId: String,
    val input: List<TurnInput>,
)

@ExperimentalCodexApi
@Serializable
data class ThreadQueueAddParams(
    val threadId: ThreadId,
    val clientUserMessageId: String,
    val input: List<TurnInput>,
)

@ExperimentalCodexApi
@Serializable
data class ThreadQueueListParams(
    val threadId: ThreadId,
    val cursor: CodexCursor? = null,
    val limit: Int? = null,
)

@ExperimentalCodexApi
@Serializable
data class ThreadQueueListResult(
    val data: List<QueuedSubmission>,
    val nextCursor: CodexCursor? = null,
)

@ExperimentalCodexApi
@Serializable
data class ThreadQueueUpdateParams(
    val threadId: ThreadId,
    val queuedSubmissionId: String,
    val input: List<TurnInput>,
)

@ExperimentalCodexApi
@Serializable
data class ThreadQueueSubmissionResult(
    val queuedSubmission: QueuedSubmission,
)

@ExperimentalCodexApi
@Serializable
data class ThreadQueueDeleteParams(
    val threadId: ThreadId,
    val queuedSubmissionId: String,
)

@ExperimentalCodexApi
@Serializable
data class ThreadQueueDeleteResult(
    val deleted: Boolean,
)

@ExperimentalCodexApi
@Serializable
data class ThreadQueueReorderParams(
    val threadId: ThreadId,
    val queuedSubmissionIds: List<String>,
)

@ExperimentalCodexApi
@Serializable
data class ThreadQueueStartParams(
    val threadId: ThreadId,
    val queuedSubmissionId: String? = null,
)

@ExperimentalCodexApi
@Serializable
data class ThreadQueueStartResult(
    val turn: Turn,
)

