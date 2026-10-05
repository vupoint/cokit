package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.auth.AccountPlanType
import io.github.vupoint.cokit.client.mcp.McpServerOauthLoginParams
import io.github.vupoint.cokit.client.mcp.McpServerStatusListParams
import io.github.vupoint.cokit.protocol.CodexProtocolJson
import io.github.vupoint.cokit.protocol.JsonRpcNotification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

class Release160CompatibilityTest {
    @Test
    fun itemHistoryPreservesOpaqueCursorsAndExclusiveItemAnchors() {
        for (fixture in listOf(
            """{"threadId":"thr_1"}""",
            """{"threadId":"thr_1","cursor":"opaque-page"}""",
            """{"threadId":"thr_1","turnId":"turn_1","cursor":{"type":"item","itemId":"item_1"},"sortDirection":"asc"}""",
            """{"threadId":"thr_1","turnId":"turn_1","cursor":{"type":"item","itemId":"item_1"},"sortDirection":"desc"}""",
        )) {
            roundTrip(fixture, ThreadItemsListParams.serializer())
        }
        roundTrip(
            """{"data":[],"nextCursor":"next-page","backwardsCursor":"previous-page"}""",
            ThreadItemsListResult.serializer(),
        )
    }

    @Test
    fun itemHistoryRejectsMalformedCursors() {
        for (cursor in listOf("42", "true", "[]", "{}", """{"type":"future","itemId":"item_1"}""", """{"type":"item","itemId":42}""", """{"type":"item","itemId":""}""")) {
            assertFailsWith<SerializationException> {
                CodexProtocolJson.decodeFromString(
                    ThreadItemsListParams.serializer(),
                    """{"threadId":"thr_1","turnId":"turn_1","cursor":$cursor}""",
                )
            }
        }
    }

    @Test
    fun itemAnchorsRequireAnItemAndTurnWhileNullCursorsStartTheFirstPage() {
        for (turnId in listOf(null, TurnId(""), TurnId(" "))) {
            assertFailsWith<IllegalArgumentException> {
                ThreadItemsListParams(
                    threadId = ThreadId("thr_1"),
                    turnId = turnId,
                    cursor = ThreadItemsListCursor.ItemAnchor(ItemId("item_1")),
                )
            }
        }
        assertFailsWith<IllegalArgumentException> { ThreadItemsListCursor.ItemAnchor(ItemId("")) }
        val params = CodexProtocolJson.decodeFromString(
            ThreadItemsListParams.serializer(),
            """{"threadId":"thr_1","cursor":null}""",
        )
        assertNull(params.cursor)
    }

    @Test
    fun mcpRequestsPreserveServerAndOauthScope() {
        roundTrip(
            """{"serverName":"example","threadId":"thr_1","limit":1}""",
            McpServerStatusListParams.serializer(),
        )
        for (registration in listOf("auto", "cimd", "dcr")) {
            roundTrip(
                """{"name":"example","threadId":"thr_1","clientRegistration":"$registration"}""",
                McpServerOauthLoginParams.serializer(),
            )
        }
        roundTrip("""{"name":"example"}""", McpServerOauthLoginParams.serializer())
        roundTrip("{}", McpServerStatusListParams.serializer())
    }

    @Test
    fun mcpItemsPreserveExplicitPresentationWithoutInventingDefaults() {
        for (presentation in listOf(
            "",
            """, "mcpAppResourceUri":"ui://example/widget"""",
            """, "mcpAppUi":{"resourceUri":"ui://example/widget","preferredModelDisplayMode":"inline"}""",
            """, "mcpAppUi":{"resourceUri":"ui://example/widget","preferredModelDisplayMode":"fullscreen"}""",
            """, "mcpAppUi":{"resourceUri":"ui://example/widget","preferredModelDisplayMode":"future-mode"}""",
        )) {
            val fixture = """{"id":"item_1","type":"mcpToolCall"$presentation}"""
            for (method in listOf("item/started", "item/completed")) {
                val event = JsonRpcNotification(
                    method = method,
                    params = CodexProtocolJson.parseToJsonElement(
                        """{"threadId":"thr_1","turnId":"turn_1","item":$fixture}""",
                    ),
                ).toCodexNotification()
                val item = when (event) {
                    is CodexNotification.ItemStarted -> event.item
                    is CodexNotification.ItemCompleted -> event.item
                    else -> error("MCP item did not decode: $event")
                }
                assertEquals(
                    CodexProtocolJson.parseToJsonElement(fixture),
                    CodexProtocolJson.encodeToJsonElement(ThreadItemSummary.serializer(), item),
                )
            }
        }
    }

    @Test
    fun terminalTurnsRetainNewErrorsAndTheirActualStatus() {
        for ((status, code) in listOf("failed" to "flexUnavailable", "interrupted" to "tooManyDenials")) {
            val fixture = """{"id":"turn_1","status":"$status","error":{"message":"Turn stopped","codexErrorInfo":"$code"}}"""
            val event = JsonRpcNotification(
                method = "turn/completed",
                params = CodexProtocolJson.parseToJsonElement("""{"threadId":"thr_1","turn":$fixture}"""),
            ).toCodexNotification()
            val turn = if (status == "failed") {
                assertIs<CodexNotification.TurnFailed>(event).turn
            } else {
                assertIs<CodexNotification.TurnCompleted>(event).turn
            }
            assertEquals(TurnStatus(status), turn.status)
            assertEquals("\"$code\"", turn.error?.codexErrorInfo?.toJsonString())
            roundTrip(fixture, Turn.serializer())
        }
    }

    @Test
    fun accountPlansPreserveProMaxAndFutureValues() {
        for (plan in listOf("promax", "future-plan")) {
            roundTrip("\"$plan\"", AccountPlanType.serializer())
        }
    }

    private fun <T> roundTrip(fixture: String, serializer: KSerializer<T>) {
        val expected = CodexProtocolJson.parseToJsonElement(fixture)
        val decoded = CodexProtocolJson.decodeFromJsonElement(serializer, expected)
        assertEquals(expected, CodexProtocolJson.encodeToJsonElement(serializer, decoded))
    }
}
