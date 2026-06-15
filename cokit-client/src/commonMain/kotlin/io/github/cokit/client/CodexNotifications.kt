package io.github.cokit.client

import io.github.cokit.protocol.CodexProtocolJson
import io.github.cokit.protocol.JsonRpcNotification
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

sealed interface CodexNotification {
    val method: String

    data class ThreadStarted(
        val threadId: ThreadId,
        val thread: Thread? = null,
    ) : CodexNotification {
        override val method: String = "thread/started"
    }

    data class ThreadStatusChanged(
        val threadId: ThreadId,
        val status: ThreadStatusType,
    ) : CodexNotification {
        override val method: String = "thread/status/changed"
    }

    data class TurnStarted(
        val turn: Turn,
    ) : CodexNotification {
        override val method: String = "turn/started"
    }

    data class TurnCompleted(
        val turn: Turn,
    ) : CodexNotification {
        override val method: String = "turn/completed"
    }

    data class TurnFailed(
        val turn: Turn,
    ) : CodexNotification {
        override val method: String = "turn/completed"
    }

    data class ItemStarted(
        val threadId: ThreadId,
        val turnId: TurnId,
        val item: ThreadItemSummary,
    ) : CodexNotification {
        override val method: String = "item/started"
    }

    data class ItemCompleted(
        val threadId: ThreadId,
        val turnId: TurnId,
        val item: ThreadItemSummary,
    ) : CodexNotification {
        override val method: String = "item/completed"
    }

    data class AgentMessageDelta(
        val threadId: ThreadId,
        val turnId: TurnId,
        val itemId: ItemId,
        val delta: String,
    ) : CodexNotification {
        override val method: String = "item/agentMessage/delta"
    }

    data class ReasoningSummaryTextDelta(
        val threadId: ThreadId,
        val turnId: TurnId,
        val itemId: ItemId,
        val summaryIndex: Int,
        val delta: String,
    ) : CodexNotification {
        override val method: String = "item/reasoning/summaryTextDelta"
    }

    data class Unknown(
        override val method: String,
    ) : CodexNotification
}

internal fun JsonRpcNotification.toCodexNotification(): CodexNotification {
    return when (method) {
        "thread/started" -> {
            val payload = params.decodeNotificationParams<ThreadStartedPayload>()
            val thread = payload?.thread
            val threadId = thread?.id ?: payload?.threadId

            if (threadId != null) {
                CodexNotification.ThreadStarted(threadId = threadId, thread = thread)
            } else {
                CodexNotification.Unknown(method)
            }
        }
        "thread/status/changed" -> {
            val payload = params.decodeNotificationParams<ThreadStatusChangedPayload>()
            if (payload != null) {
                CodexNotification.ThreadStatusChanged(
                    threadId = payload.threadId,
                    status = payload.status,
                )
            } else {
                CodexNotification.Unknown(method)
            }
        }
        "turn/started" -> {
            val turn = params.decodeNotificationParams<TurnPayload>()?.turn
            if (turn != null) {
                CodexNotification.TurnStarted(turn)
            } else {
                CodexNotification.Unknown(method)
            }
        }
        "turn/completed" -> {
            val turn = params.decodeNotificationParams<TurnPayload>()?.turn
            when (turn?.status) {
                TurnStatus.Failed -> CodexNotification.TurnFailed(turn)
                null -> CodexNotification.Unknown(method)
                else -> CodexNotification.TurnCompleted(turn)
            }
        }
        "item/started" -> {
            val payload = params.decodeNotificationParams<ItemPayload>()
            if (payload != null) {
                CodexNotification.ItemStarted(
                    threadId = payload.threadId,
                    turnId = payload.turnId,
                    item = payload.item,
                )
            } else {
                CodexNotification.Unknown(method)
            }
        }
        "item/completed" -> {
            val payload = params.decodeNotificationParams<ItemPayload>()
            if (payload != null) {
                CodexNotification.ItemCompleted(
                    threadId = payload.threadId,
                    turnId = payload.turnId,
                    item = payload.item,
                )
            } else {
                CodexNotification.Unknown(method)
            }
        }
        "item/agentMessage/delta" -> {
            val payload = params.decodeNotificationParams<ItemDeltaPayload>()
            if (payload != null) {
                CodexNotification.AgentMessageDelta(
                    threadId = payload.threadId,
                    turnId = payload.turnId,
                    itemId = payload.itemId,
                    delta = payload.delta,
                )
            } else {
                CodexNotification.Unknown(method)
            }
        }
        "item/reasoning/summaryTextDelta" -> {
            val payload = params.decodeNotificationParams<ReasoningSummaryTextDeltaPayload>()
            if (payload != null) {
                CodexNotification.ReasoningSummaryTextDelta(
                    threadId = payload.threadId,
                    turnId = payload.turnId,
                    itemId = payload.itemId,
                    summaryIndex = payload.summaryIndex,
                    delta = payload.delta,
                )
            } else {
                CodexNotification.Unknown(method)
            }
        }
        else -> CodexNotification.Unknown(method)
    }
}

@Serializable
private data class ThreadStartedPayload(
    val threadId: ThreadId? = null,
    val thread: Thread? = null,
)

@Serializable
private data class ThreadStatusChangedPayload(
    val threadId: ThreadId,
    val status: ThreadStatusType,
)

@Serializable
private data class TurnPayload(
    val turn: Turn,
)

@Serializable
private data class ItemPayload(
    val threadId: ThreadId,
    val turnId: TurnId,
    val item: ThreadItemSummary,
)

@Serializable
private data class ItemDeltaPayload(
    val threadId: ThreadId,
    val turnId: TurnId,
    val itemId: ItemId,
    val delta: String,
)

@Serializable
private data class ReasoningSummaryTextDeltaPayload(
    val threadId: ThreadId,
    val turnId: TurnId,
    val itemId: ItemId,
    val summaryIndex: Int,
    val delta: String,
)

private inline fun <reified T> JsonElement?.decodeNotificationParams(): T? {
    val element = this ?: return null
    return runCatching {
        CodexProtocolJson.decodeFromJsonElement<T>(element)
    }.getOrNull()
}
