package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.models.ModelProviderCapabilities
import io.github.vupoint.cokit.client.mcp.McpServerOauthLoginResult
import io.github.vupoint.cokit.client.policy.ManagedPolicyReadResult
import io.github.vupoint.cokit.client.skills.SkillMetadata
import io.github.vupoint.cokit.protocol.CodexProtocolJson
import io.github.vupoint.cokit.protocol.JsonRpcNotification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@OptIn(ExperimentalCodexApi::class)
class Release163CompatibilityTest {
    @Test
    fun providerCapabilitiesDecodeWithoutRetiredNamespaceTools() {
        roundTrip(
            """{"webSearch":true,"imageGeneration":false}""",
            ModelProviderCapabilities.serializer(),
        )
    }

    @Test
    fun threadListPreservesExclusionsWithoutTruncatingOrDeduplicating() {
        roundTrip(
            """{"excludedThreadIds":["thr_1","thr_1"],"cursor":"next","limit":1}""",
            ThreadListParams.serializer(),
        )
        roundTrip("""{"excludedThreadIds":[]}""", ThreadListParams.serializer())
        roundTrip("{}", ThreadListParams.serializer())
    }

    @Test
    fun goalsAndDelegatedTurnsPreserveProvenanceWithoutInventingAuthorization() {
        for (origin in listOf("user", "automatic", "future-origin")) {
            roundTrip("""{"threadId":"thr_1","origin":"$origin"}""", ThreadGoalSetParams.serializer())
            roundTrip("""{"threadId":"thr_1","origin":"$origin"}""", ThreadGoalClearParams.serializer())
        }
        roundTrip("""{"threadId":"thr_1"}""", ThreadGoalSetParams.serializer())
        roundTrip("""{"threadId":"thr_1"}""", ThreadGoalClearParams.serializer())
        roundTrip(
            """{"threadId":"thr_1","input":[],"parentTurnId":"turn_parent","rootTurnId":"turn_root","toolOutput":{"name":"example","output":"done","retain":true}}""",
            TurnStartParams.serializer(),
        )
        roundTrip("""{"name":"example","output":"done"}""", TurnToolOutput.serializer())
        val turn = """{"id":"turn_1","status":"completed","rootTurnId":"turn_root"}"""
        roundTrip(turn, Turn.serializer())
        val event = notification("turn/completed", """{"threadId":"thr_1","turn":$turn}""")
        assertEquals(TurnId("turn_root"), assertIs<CodexNotification.TurnCompleted>(event).turn.rootTurnId)
    }

    @Test
    fun oauthCorrelatesLoginAttemptsAndRedactsDiagnostics() {
        val result = """{"authorizationUrl":"https://example.invalid/oauth","loginId":"login_1"}"""
        roundTrip(result, McpServerOauthLoginResult.serializer())
        roundTrip("""{"authorizationUrl":"https://example.invalid/oauth"}""", McpServerOauthLoginResult.serializer())
        assertFalse(CodexProtocolJson.decodeFromString(McpServerOauthLoginResult.serializer(), result).toString().contains("example.invalid"))
        val event = assertIs<CodexNotification.McpServerOauthLoginCompleted>(notification(
            "mcpServer/oauthLogin/completed",
            """{"name":"example","success":false,"loginId":"login_1","threadId":"thr_1","error":"private-diagnostic"}""",
        ))
        assertEquals("login_1", event.loginId?.value)
        assertEquals(ThreadId("thr_1"), event.threadId)
        assertFalse(event.toString().contains("private-diagnostic"))
        val legacy = assertIs<CodexNotification.McpServerOauthLoginCompleted>(notification(
            "mcpServer/oauthLogin/completed", """{"name":"example","success":true}""",
        ))
        assertNull(legacy.loginId)
    }

    @Test
    fun managedRequirementsPreserveIndependentSpeedGatesAndRedactHeaderValues() {
        val fixture = """{"requirements":{"browserUse":{"extension":{"requestHeaders":[{"name":"X-Example","value":"private-value"}]}}},"supportsIndependentSpeedModes":true}"""
        roundTrip(fixture, ManagedPolicyReadResult.serializer())
        val result = CodexProtocolJson.decodeFromString(ManagedPolicyReadResult.serializer(), fixture)
        assertEquals(true, result.supportsIndependentSpeedModes)
        assertFalse(result.toString().contains("private-value"))
        assertNull(CodexProtocolJson.decodeFromString(ManagedPolicyReadResult.serializer(), "{}").supportsIndependentSpeedModes)
        roundTrip("""{"supportsIndependentSpeedModes":false}""", ManagedPolicyReadResult.serializer())
    }

    @Test
    fun receivedMetadataRetainsRemotePathsAndSubagentConfiguration() {
        roundTrip(
            """{"name":"example","description":"Example","enabled":true,"path":"file://remote/path/to/SKILL.md","scope":"user"}""",
            SkillMetadata.serializer(),
        )
        val fixture = """{"id":"item_1","type":"subAgentActivity","model":"example-model","reasoningEffort":"high"}"""
        for (method in listOf("item/started", "item/completed")) {
            val event = notification(method, """{"threadId":"thr_1","turnId":"turn_1","item":$fixture}""")
            val item = when (event) {
                is CodexNotification.ItemStarted -> event.item
                is CodexNotification.ItemCompleted -> event.item
                else -> error("Expected item lifecycle event")
            }
            assertEquals(ModelName("example-model"), item.model)
            assertEquals(ReasoningEffort.High, item.reasoningEffort)
        }
    }

    @Test
    fun readReceiptsDistinguishUnavailableReadAndUnreadAndPreserveFuturePositions() {
        val unavailable = CodexProtocolJson.decodeFromString(ThreadReadResult.serializer(), """{"thread":{"id":"thr_1"}}""")
        assertNull(unavailable.readState)
        val read = CodexProtocolJson.decodeFromString(ThreadReadResult.serializer(), """{"thread":{"id":"thr_1"},"readState":{"revision":"rev_1","firstUnread":null}}""")
        assertNull(requireNotNull(read.readState).firstUnread)
        for (position in listOf("""{"type":"threadStart"}""", """{"type":"turn","turnId":"turn_1"}""", """{"type":"future","opaque":{"value":1}}""")) {
            val receipt = """{"revision":"rev_1","firstUnread":$position}"""
            roundTrip(receipt, ThreadReadState.serializer())
            val event = assertIs<CodexNotification.ThreadReadStateChanged>(notification(
                "thread/readState/changed", """{"threadId":"thr_1","readState":$receipt}""",
            ))
            assertEquals(ThreadReadStateRevision("rev_1"), event.readState.revision)
        }
        for (operation in listOf("read", "unread")) {
            roundTrip(
                """{"threadId":"thr_1","expectedRevision":"rev_1","operation":{"type":"$operation"}}""",
                ThreadReadStateUpdateParams.serializer(),
            )
        }
    }

    @Test
    fun predictionOutcomesPreserveFutureVariantsAndRejectMalformedKnownPayloads() {
        for (result in listOf("""{"type":"completed","text":"next"}""", """{"type":"completed"}""", """{"type":"failed"}""", """{"type":"future","opaque":[1]}""")) {
            roundTrip(result, ThreadPredictionResult.serializer())
            assertIs<CodexNotification.ThreadPredictionUpdated>(notification(
                "thread/prediction/updated", """{"threadId":"thr_1","sourceTurnId":"turn_1","result":$result}""",
            ))
        }
        for (malformed in listOf("[]", "{}", """{"type":2}""", """{"type":"completed","text":{}}""", """{"type":"completed","text":2}""")) {
            assertFailsWith<SerializationException> { CodexProtocolJson.decodeFromString(ThreadPredictionResult.serializer(), malformed) }
            assertIs<CodexNotification.Unknown>(notification(
                "thread/prediction/updated", """{"threadId":"thr_1","sourceTurnId":"turn_1","result":$malformed}""",
            ))
        }
        for (malformed in listOf("[]", "{}", """{"type":2}""", """{"type":"turn"}""", """{"type":"turn","turnId":2}""")) {
            assertFailsWith<SerializationException> { CodexProtocolJson.decodeFromString(ThreadUnreadPosition.serializer(), malformed) }
        }
    }

    @Test
    fun itemReadsBoundRequestsAndRetainUnknownItemPayloads() {
        for (size in listOf(0, 101)) {
            assertFailsWith<IllegalArgumentException> {
                ThreadItemsReadParams(ThreadId("thr_1"), TurnId("turn_1"), List(size) { ItemId("item_$it") })
            }
        }
        ThreadItemsReadParams(ThreadId("thr_1"), TurnId("turn_1"), List(100) { ItemId("item_$it") })
        roundTrip(
            """{"data":[{"turnId":"turn_1","item":{"type":"future","id":"item_1","opaque":[1]}}]}""",
            ThreadItemsReadResult.serializer(),
        )
    }

    private fun notification(method: String, params: String): CodexNotification =
        JsonRpcNotification(method, CodexProtocolJson.parseToJsonElement(params)).toCodexNotification()

    private fun <T> roundTrip(fixture: String, serializer: KSerializer<T>) {
        val expected = CodexProtocolJson.parseToJsonElement(fixture)
        val decoded = CodexProtocolJson.decodeFromJsonElement(serializer, expected)
        assertEquals(expected, CodexProtocolJson.encodeToJsonElement(serializer, decoded))
    }
}
