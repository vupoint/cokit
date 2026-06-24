package io.github.vupoint.cokit.sample.cli

import io.github.vupoint.cokit.client.CodexNotification
import io.github.vupoint.cokit.client.ItemType
import io.github.vupoint.cokit.client.Turn
import io.github.vupoint.cokit.client.TurnId
import kotlinx.coroutines.channels.Channel

internal suspend fun streamAssistantResponse(
    turnId: TurnId,
    events: Channel<CodexNotification>,
    output: SampleOutput,
) {
    var wroteAssistantText = false
    while (true) {
        val event = events.receiveCatching().getOrNull()
            ?: throw SampleRunException("Turn ${turnId.value} ended before completion.")
        when (event) {
            is CodexNotification.AgentMessageDelta -> {
                if (event.turnId == turnId) {
                    output.text(event.delta)
                    wroteAssistantText = true
                }
            }

            is CodexNotification.ItemCompleted -> {
                val text = event.item.text
                if (
                    event.turnId == turnId &&
                    event.item.type == ItemType.AgentMessage &&
                    !wroteAssistantText &&
                    !text.isNullOrBlank()
                ) {
                    output.text(text)
                    wroteAssistantText = true
                }
            }

            is CodexNotification.TurnCompleted -> {
                if (event.turn.id == turnId) {
                    if (wroteAssistantText) {
                        output.line()
                    } else {
                        output.line("(no assistant message)")
                    }
                    return
                }
            }

            is CodexNotification.TurnFailed -> {
                if (event.turn.id == turnId) {
                    throw SampleRunException(event.turn.failureMessage())
                }
            }

            is CodexNotification.Error -> {
                if (event.turnId == turnId && !event.willRetry) {
                    throw SampleRunException(
                        "Turn ${turnId.value} failed: ${event.error.message}",
                    )
                }
            }

            else -> Unit
        }
    }
}

private fun Turn.failureMessage(): String {
    val message = error?.message ?: "unknown error"
    return "Turn ${id.value} failed: $message"
}

internal class SampleRunException(message: String) : RuntimeException(message)
