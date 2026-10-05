package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.tools.*
import io.github.vupoint.cokit.protocol.JsonRpcNotification
import io.github.vupoint.cokit.protocol.CodexProtocolJson
import io.github.vupoint.cokit.protocol.JsonRpcId
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, ExperimentalCodexApi::class)
class DynamicToolCallTest {
    @Test
    fun unhandledCallsReturnSchemaValidFailure() = runTest {
        for (experimental in listOf(false, true)) {
            val (_, transport) = connect(experimental)
            transport.receive(callRequest())
            runCurrent()
            val response = transport.sent.last() as JsonRpcResponse
            assertEquals(JsonRpcId.StringId("rpc_1"), response.id)
            assertEquals(json("""{"contentItems":[],"success":false}"""), response.result)
        }
    }

    @Test
    fun threadStartSendsFunctionAndNamespaceDefinitionsThroughBothApis() = runTest {
        val (client, transport) = connect(experimental = true)
        val tools = listOf(
            DynamicToolSpec.Function("lookup", "Look up a record", CodexJsonPayload.parse("""{"type":"object","future":true}""")),
            DynamicToolSpec.Namespace("catalog", "Catalog tools", listOf(
                DynamicToolNamespaceTool.Function("search", "Search records", CodexJsonPayload.parse("{}"), deferLoading = true),
            )),
        )
        val expected = json("""{"dynamicTools":[{"type":"function","name":"lookup","description":"Look up a record","inputSchema":{"type":"object","future":true}},{"type":"namespace","name":"catalog","description":"Catalog tools","tools":[{"type":"function","name":"search","description":"Search records","inputSchema":{},"deferLoading":true}]}]}""")
        for (typedApi in listOf(true, false)) {
            val pending = async {
                if (typedApi) client.threads.start(StartThreadRequest(dynamicTools = tools))
                else client.request(CodexRpc.Thread.Start, ThreadStartParams(dynamicTools = tools)).thread
            }
            runCurrent()
            val request = transport.sent.last() as JsonRpcRequest
            assertEquals("thread/start", request.method)
            assertEquals(expected, request.params)
            transport.receive(JsonRpcResponse(request.id, json("""{"thread":{"id":"thr_1"}}""")))
            assertEquals(ThreadId("thr_1"), pending.await().id)
        }
    }

    @Test
    fun experimentalInputsAndHandlerRegistrationAreRejectedBeforeSending() = runTest {
        val (client, transport) = connect()
        val before = transport.sent.size
        for (tools in listOf(emptyList(), listOf(DynamicToolSpec.Function("lookup", "Lookup", CodexJsonPayload.parse("{}"))))) {
            assertFailsWith<IllegalArgumentException> { client.threads.start(StartThreadRequest(dynamicTools = tools)) }
            assertFailsWith<IllegalArgumentException> { client.request(CodexRpc.Thread.Start, ThreadStartParams(dynamicTools = tools)) }
            assertFailsWith<IllegalArgumentException> { client.request(CodexRpc.Thread.Start, ThreadStartParams(dynamicTools = tools)) {} }
        }
        assertFailsWith<IllegalArgumentException> {
            client.registerDynamicToolCallHandler { error("must not run") }
        }
        assertEquals(before, transport.sent.size)
    }

    @Test
    fun stableThreadStartOmitsDynamicToolsWithoutOptIn() = runTest {
        val (client, transport) = connect()
        val pending = async { client.threads.start() }
        runCurrent()
        val request = transport.sent.last() as JsonRpcRequest
        assertEquals(json("{}"), request.params)
        transport.receive(JsonRpcResponse(request.id, json("""{"thread":{"id":"thr_1"}}""")))
        pending.await()
    }

    @Test
    fun handlerReceivesTypedCallsAndReturnsContentUsingTheRpcId() = runTest {
        val (client, transport) = connect(experimental = true)
        val observed = async { client.serverRequests.first() }
        var handled: DynamicToolCallRequest? = null
        client.registerDynamicToolCallHandler {
            handled = it
            DynamicToolCallResponse(listOf(
                DynamicToolCallOutputContent.Text("Found"),
                DynamicToolCallOutputContent.Image("data:image/png;base64,AA=="),
                DynamicToolCallOutputContent.Audio("data:audio/wav;base64,AA=="),
            ), success = true)
        }
        transport.receive(callRequest("""{"threadId":"thr_1","turnId":"turn_1","callId":"call_1","namespace":"catalog","tool":"lookup","arguments":{"future":[1,null]},"futureField":true}"""))
        runCurrent()
        val event = assertIs<CodexServerRequest.DynamicToolCall>(observed.await())
        assertEquals(handled, event.request)
        assertEquals(ThreadId("thr_1"), event.request.threadId)
        assertEquals(TurnId("turn_1"), event.request.turnId)
        assertEquals("call_1", event.request.callId)
        assertEquals("catalog", event.request.namespace)
        assertEquals("lookup", event.request.tool)
        assertEquals("""{"future":[1,null]}""", event.request.arguments.toJsonString())
        val response = transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.StringId("rpc_1"), response.id)
        assertEquals(json("""{"contentItems":[{"type":"inputText","text":"Found"},{"type":"inputImage","imageUrl":"data:image/png;base64,AA=="},{"type":"inputAudio","audioUrl":"data:audio/wav;base64,AA=="}],"success":true}"""), response.result)
    }

    @Test
    fun malformedCallsNeverReachTheHandlerButNullArgumentsRemainValid() = runTest {
        val (client, transport) = connect(experimental = true)
        var calls = 0
        client.registerDynamicToolCallHandler {
            calls++
            assertEquals("null", it.arguments.toJsonString())
            DynamicToolCallResponse(emptyList(), success = false)
        }
        val valid = """{"threadId":"thr_1","turnId":"turn_1","callId":"call_1","tool":"lookup","arguments":null}"""
        val fields = json(valid).jsonObject
        val invalid = fields.keys.map { key -> kotlinx.serialization.json.JsonObject(fields - key).toString() } + listOf("null", "[]", "{}")
        for (params in invalid) {
            transport.receive(callRequest(params))
            runCurrent()
            assertEquals(-32602, (transport.sent.last() as JsonRpcResponse).error?.code)
        }
        assertEquals(0, calls)
        transport.receive(callRequest(valid))
        runCurrent()
        assertEquals(1, calls)
        assertEquals(json("""{"contentItems":[],"success":false}"""), (transport.sent.last() as JsonRpcResponse).result)
    }

    @Test
    fun handlerFailuresDoNotExposePrivateDiagnosticsAndLaterCallsStillWork() = runTest {
        val (client, transport) = connect(experimental = true)
        client.registerDynamicToolCallHandler { error("private-handler-diagnostic") }
        transport.receive(callRequest())
        runCurrent()
        val response = transport.sent.last() as JsonRpcResponse
        assertEquals(-32000, response.error?.code)
        assertFalse(response.toString().contains("private-handler-diagnostic"))
        client.registerDynamicToolCallHandler { DynamicToolCallResponse(emptyList(), false) }
        transport.receive(callRequest().copy(id = JsonRpcId.Number(2)))
        runCurrent()
        assertEquals(JsonRpcId.Number(2), (transport.sent.last() as JsonRpcResponse).id)
        assertEquals(json("""{"contentItems":[],"success":false}"""), (transport.sent.last() as JsonRpcResponse).result)
    }

    @Test
    fun handlerTimeoutReturnsFailureWithoutStoppingLaterRequests() = runTest {
        val (client, transport) = connect(experimental = true)
        client.registerDynamicToolCallHandler { withTimeout(1) { awaitCancellation() } }
        transport.receive(callRequest())
        runCurrent()
        advanceTimeBy(1)
        runCurrent()
        assertEquals(-32000, (transport.sent.last() as? JsonRpcResponse)?.error?.code)

        client.registerDynamicToolCallHandler { DynamicToolCallResponse(emptyList(), true) }
        transport.receive(callRequest().copy(id = JsonRpcId.Number(2)))
        runCurrent()
        val response = transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(2), response.id)
        assertEquals(json("""{"contentItems":[],"success":true}"""), response.result)
    }

    @Test
    fun closingClientCancelsSuspendedHandlerWithoutSendingAFailure() = runTest {
        val (client, transport) = connect(experimental = true)
        val entered = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        client.registerDynamicToolCallHandler {
            entered.complete(Unit)
            try { awaitCancellation() } finally { cancelled.complete(Unit) }
        }
        transport.receive(callRequest())
        runCurrent()
        assertTrue(entered.isCompleted)
        val before = transport.sent.size
        client.close()
        runCurrent()
        assertTrue(cancelled.isCompleted)
        assertEquals(before, transport.sent.size)
    }

    @Test
    fun dynamicToolLifecyclePreservesContentAndFutureVariants() = runTest {
        val (client, transport) = connect(experimental = true)
        for (method in listOf("item/started", "item/completed")) {
            val notification = async { client.notifications.first() }
            transport.receive(JsonRpcNotification(method, json("""{"threadId":"thr_1","turnId":"turn_1","item":{"id":"call_1","type":"dynamicToolCall","status":"completed","tool":"lookup","namespace":"catalog","arguments":{"future":true},"contentItems":[{"type":"inputText","text":"Found"},{"type":"futureContent","value":42}],"success":true}}""")))
            runCurrent()
            val item = when (val event = notification.await()) {
                is CodexNotification.ItemStarted -> event.item
                is CodexNotification.ItemCompleted -> event.item
                else -> error("Expected item notification")
            }
            assertEquals(ItemType.DynamicToolCall, item.type)
            assertEquals("lookup", item.tool)
            assertEquals("catalog", item.namespace)
            assertEquals(CodexJsonPayload.parse("""{"future":true}"""), item.arguments)
            assertEquals(true, item.success)
            assertEquals(DynamicToolCallOutputContent.Text("Found"), item.contentItems?.first())
            val future = assertIs<DynamicToolCallOutputContent.Unknown>(item.contentItems?.last())
            assertEquals(json("""{"type":"futureContent","value":42}"""), CodexProtocolJson.encodeToJsonElement(DynamicToolCallOutputContent.serializer(), future))
        }
    }

    @Test
    fun requestAndResponseDiagnosticsHideToolPayloads() {
        val request = CodexProtocolJson.decodeFromString(DynamicToolCallRequest.serializer(),
            """{"threadId":"thr_1","turnId":"turn_1","callId":"call_1","tool":"lookup","arguments":{"secret":"private-value"}}""")
        val response = DynamicToolCallResponse(listOf(DynamicToolCallOutputContent.Text("private-value")), true)
        assertFalse(request.toString().contains("private-value"))
        assertFalse(response.toString().contains("private-value"))
    }

    private suspend fun TestScope.connect(experimental: Boolean = false): Pair<CodexClient, FakeJsonRpcTransport> {
        val transport = FakeJsonRpcTransport()
        val pending = async {
            CodexClients.connect(CodexClientConnection(
                transport, ClientInfo("cokit_test", "CoKit Test", "0.1.0"), backgroundScope,
                InitializeCapabilities(experimentalApi = experimental),
            ))
        }
        runCurrent()
        val initialize = transport.sent.single() as JsonRpcRequest
        transport.receive(JsonRpcResponse(initialize.id, json("{}")))
        return pending.await() to transport
    }

    private fun callRequest(params: String = """{"threadId":"thr_1","turnId":"turn_1","callId":"call_1","tool":"lookup","arguments":{"future":[1,null]}}""") =
        JsonRpcRequest(JsonRpcId.StringId("rpc_1"), "item/tool/call", json(params))

    private fun json(value: String) = CodexProtocolJson.parseToJsonElement(value)
}
