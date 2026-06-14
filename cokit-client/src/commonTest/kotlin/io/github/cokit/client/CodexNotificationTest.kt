package io.github.cokit.client

import io.github.cokit.protocol.JsonRpcNotification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CodexNotificationTest {
    @Test
    fun decodesCoreThreadNotificationsAsTypedModels() {
        val started = JsonRpcNotification(
            method = "thread/started",
            params = buildJsonObject {
                put(
                    "thread",
                    buildJsonObject {
                        put("id", "thr_123")
                        put("preview", "Review changes")
                    },
                )
            },
        ).toCodexNotification()

        val threadStarted = assertIs<CodexNotification.ThreadStarted>(started)
        assertEquals(ThreadId("thr_123"), threadStarted.threadId)
        assertEquals("Review changes", threadStarted.thread?.preview)

        val statusChanged = JsonRpcNotification(
            method = "thread/status/changed",
            params = buildJsonObject {
                put("threadId", "thr_123")
                put("status", "inProgress")
            },
        ).toCodexNotification()

        val threadStatusChanged = assertIs<CodexNotification.ThreadStatusChanged>(statusChanged)
        assertEquals(ThreadId("thr_123"), threadStatusChanged.threadId)
        assertEquals(ThreadStatusType("inProgress"), threadStatusChanged.status)
    }

    @Test
    fun decodesTurnLifecycleNotificationsAsTypedModels() {
        val started = JsonRpcNotification(
            method = "turn/started",
            params = turnParams("turn_123", "inProgress"),
        ).toCodexNotification()

        val turnStarted = assertIs<CodexNotification.TurnStarted>(started)
        assertEquals(TurnId("turn_123"), turnStarted.turn.id)
        assertEquals(TurnStatus.InProgress, turnStarted.turn.status)

        val completed = JsonRpcNotification(
            method = "turn/completed",
            params = turnParams("turn_123", "completed"),
        ).toCodexNotification()

        val turnCompleted = assertIs<CodexNotification.TurnCompleted>(completed)
        assertEquals(TurnId("turn_123"), turnCompleted.turn.id)
        assertEquals(TurnStatus.Completed, turnCompleted.turn.status)

        val failed = JsonRpcNotification(
            method = "turn/completed",
            params = buildJsonObject {
                put(
                    "turn",
                    buildJsonObject {
                        put("id", "turn_456")
                        put("status", "failed")
                        put("items", buildJsonArray {})
                        put(
                            "error",
                            buildJsonObject {
                                put("message", "model request failed")
                            },
                        )
                    },
                )
            },
        ).toCodexNotification()

        val turnFailed = assertIs<CodexNotification.TurnFailed>(failed)
        assertEquals("turn/completed", turnFailed.method)
        assertEquals(TurnId("turn_456"), turnFailed.turn.id)
        assertEquals(TurnStatus.Failed, turnFailed.turn.status)
        assertEquals("model request failed", turnFailed.turn.error?.message)
    }

    @Test
    fun unknownNotificationKeepsOnlyMethodName() {
        val notification = JsonRpcNotification(
            method = "item/agentMessage/delta",
            params = buildJsonObject {
                put("delta", "hello")
            },
        ).toCodexNotification()

        assertEquals(CodexNotification.Unknown("item/agentMessage/delta"), notification)
    }

    private fun turnParams(turnId: String, status: String) = buildJsonObject {
        put(
            "turn",
            buildJsonObject {
                put("id", turnId)
                put("status", status)
                put("items", buildJsonArray {})
            },
        )
    }
}
