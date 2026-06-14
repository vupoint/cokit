package io.github.cokit.client

import io.github.cokit.protocol.JsonRpcNotification
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface CodexNotification {
    val method: String

    data class ThreadStarted(
        val threadId: ThreadId,
    ) : CodexNotification {
        override val method: String = "thread/started"
    }

    data class Unknown(
        override val method: String,
    ) : CodexNotification
}

internal fun JsonRpcNotification.toCodexNotification(): CodexNotification {
    return when (method) {
        "thread/started" -> {
            val obj = params?.jsonObject
            val threadId = obj
                ?.get("threadId")
                ?.jsonPrimitive
                ?.contentOrNull
                ?: obj
                    ?.get("thread")
                    ?.jsonObject
                    ?.get("id")
                    ?.jsonPrimitive
                    ?.contentOrNull

            if (threadId != null) {
                CodexNotification.ThreadStarted(ThreadId(threadId))
            } else {
                CodexNotification.Unknown(method)
            }
        }
        else -> CodexNotification.Unknown(method)
    }
}
