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

private inline fun <reified T> JsonElement?.decodeNotificationParams(): T? {
    val element = this ?: return null
    return runCatching {
        CodexProtocolJson.decodeFromJsonElement<T>(element)
    }.getOrNull()
}
