package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.auth.AccountReadResult
import io.github.vupoint.cokit.client.extensions.HooksListParams
import io.github.vupoint.cokit.client.extensions.HooksListResult
import io.github.vupoint.cokit.client.policy.ManagedPolicyReadResult
import io.github.vupoint.cokit.protocol.CodexProtocolJson
import io.github.vupoint.cokit.protocol.JsonRpcNotification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

class Release167CompatibilityTest {
    @Test
    fun workspaceNamesSurviveAccountReadsWithoutEnteringDiagnostics() {
        val payload = json("""{"requiresOpenaiAuth":true,"account":{"type":"chatgpt","email":"example@example.invalid","planType":"team","workspaceName":"Example workspace"}}""")
        val result = CodexProtocolJson.decodeFromJsonElement<AccountReadResult>(payload)
        assertEquals(payload, CodexProtocolJson.encodeToJsonElement(result))
        assertFalse(result.toString().contains("Example workspace"))
    }

    @Test
    fun hookDiscoveryRetainsThreadScopeAndEnvironmentIdentity() {
        val params = json("""{"threadId":"thr_1"}""")
        assertEquals(params, CodexProtocolJson.encodeToJsonElement(CodexProtocolJson.decodeFromJsonElement<HooksListParams>(params)))
        val payload = json("""{"data":[{"cwd":"/path/to/project","hooks":[{"key":"example","eventName":"preToolUse","handlerType":"command","source":"user","sourcePath":"/path/to/hooks.json","enabled":true,"timeoutSec":30,"trustStatus":"trusted","currentHash":"example","displayOrder":0,"isManaged":false,"environmentId":"env_1"}]}]}""")
        assertEquals(payload, CodexProtocolJson.encodeToJsonElement(CodexProtocolJson.decodeFromJsonElement<HooksListResult>(payload)))
    }

    @Test
    fun managedTelemetryPreservesExporterAndFutureFieldsWithoutLoggingThem() {
        val payload = json("""{"requirements":{"otel":{"environment":"example-private-environment","exporter":{"otlp-http":{"endpoint":"https://example.invalid/telemetry","headers":{"example":"<redacted>"},"protocol":"binary"}},"span_attributes":{"example":"value"},"tracestate":{"vendor":{"tenant":"example"}},"future":{"enabled":true}}}}""")
        val result = CodexProtocolJson.decodeFromJsonElement<ManagedPolicyReadResult>(payload)
        assertEquals(payload, CodexProtocolJson.encodeToJsonElement(result))
        assertFalse(result.toString().contains("example-private-environment"))
    }

    @Test
    fun itemLifecycleRetainsMillisecondTimestampsAndAcceptsOlderItems() {
        val payload = json("""{"id":"item_1","type":"agentMessage","text":"Example","startedAtMs":1700000000123,"completedAtMs":1700000000456}""")
        assertEquals(payload, CodexProtocolJson.encodeToJsonElement(CodexProtocolJson.decodeFromJsonElement<ThreadItemSummary>(payload)))
        for (method in listOf("item/started", "item/completed")) {
            val notification = JsonRpcNotification(method, json("""{"threadId":"thr_1","turnId":"turn_1","item":$payload}""")).toCodexNotification()
            assertFalse(notification is CodexNotification.Unknown)
        }
        for (timestamps in listOf("", ",\"startedAtMs\":null,\"completedAtMs\":null")) {
            CodexProtocolJson.decodeFromString(ThreadItemSummary.serializer(), """{"id":"item_1","type":"agentMessage"$timestamps}""")
        }
    }

    @Test
    fun sectionMovesAndRemovalDecodeButMalformedEventsRemainUnknown() {
        for (section in listOf("\"section_1\"", "null")) {
            val event = JsonRpcNotification("thread/section/changed", json("""{"threadId":"thr_1","sectionId":$section}""")).toCodexNotification()
            assertFalse(event is CodexNotification.Unknown)
        }
        for (payload in listOf("{}", """{"threadId":"thr_1"}""", """{"threadId":"thr_1","sectionId":[]}""")) {
            assertIs<CodexNotification.Unknown>(JsonRpcNotification("thread/section/changed", json(payload)).toCodexNotification())
        }
    }

    private fun json(value: String) = CodexProtocolJson.parseToJsonElement(value)
}
