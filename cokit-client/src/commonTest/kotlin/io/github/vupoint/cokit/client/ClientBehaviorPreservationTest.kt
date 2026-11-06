package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.protocol.CodexProtocolJson
import io.github.vupoint.cokit.protocol.JsonRpcErrorObject
import io.github.vupoint.cokit.protocol.JsonRpcId
import io.github.vupoint.cokit.protocol.JsonRpcMessage
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.rpc.JsonRpcTransport
import io.github.vupoint.cokit.rpc.JsonRpcRemoteException
import io.github.vupoint.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonNull

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, ExperimentalCodexApi::class)
class ClientBehaviorPreservationTest {
    @Test
    fun failedHandshakeClosesTransportWithoutCancellingCallerScope() = runTest {
        val fake = FakeJsonRpcTransport()
        var closes = 0
        val transport = object : JsonRpcTransport by fake {
            override fun close() {
                closes++
                fake.close()
            }
        }
        val pending = async { runCatching {
            CodexClients.connect(CodexClientConnection(
                transport, ClientInfo("cokit_test", "CoKit Test", "0.1.0"), backgroundScope,
            ))
        } }
        runCurrent()
        val initialize = fake.sent.single() as JsonRpcRequest
        fake.receive(JsonRpcResponse(initialize.id, error = JsonRpcErrorObject(-32600, "Example initialization failure")))
        assertEquals(-32600, assertIs<JsonRpcRemoteException>(pending.await().exceptionOrNull()).error.code)
        assertEquals(1, closes)
        assertEquals(listOf(initialize), fake.sent)
        assertTrue(backgroundScope.isActive)
    }

    @Test
    fun cancelledHandshakeClosesTransportWithoutCancellingCallerScope() = runTest {
        val fake = FakeJsonRpcTransport()
        var closes = 0
        val transport = object : JsonRpcTransport by fake {
            override fun close() {
                closes++
                fake.close()
            }
        }
        val pending = async {
            CodexClients.connect(CodexClientConnection(
                transport, ClientInfo("cokit_test", "CoKit Test", "0.1.0"), backgroundScope,
            ))
        }
        runCurrent()
        val initialize = fake.sent.single() as JsonRpcRequest
        pending.cancelAndJoin()
        assertTrue(pending.isCancelled)
        assertEquals(1, closes)
        assertEquals(listOf(initialize), fake.sent)
        assertTrue(backgroundScope.isActive)
    }

    @Test
    fun forkApisPreservePresentFalseOptionsAndReturnTheSameProjection() = runTest {
        val (client, transport) = connect(experimental = true)
        for (convenience in listOf(false, true)) {
            val pending = async {
                if (convenience) client.threads.fork(ForkThreadRequest(
                    ThreadId("thr_source"), attachToSource = ThreadAttachmentMode(false),
                    experimentalDisablePredictionMcp = ThreadPredictionMcpDisabled(false),
                )) else client.request(CodexRpc.Thread.Fork, ThreadForkParams(
                    ThreadId("thr_source"), attachToSource = ThreadAttachmentMode(false),
                    experimentalDisablePredictionMcp = ThreadPredictionMcpDisabled(false),
                )).thread
            }
            runCurrent()
            val request = transport.sent.last() as JsonRpcRequest
            assertEquals("thread/fork", request.method)
            assertEquals(json("""{"threadId":"thr_source","attachToSource":false,"experimentalDisablePredictionMcp":false}"""), request.params)
            transport.receive(JsonRpcResponse(request.id, json("""{"thread":{"id":"thr_fork"}}""")))
            assertEquals(ThreadId("thr_fork"), pending.await().id)
        }
    }

    @Test
    fun requestIdCallbackRunsAfterSendAndBeforeTheResultReturns() = runTest {
        val events = mutableListOf<String>()
        val (client, transport) = connect { fake ->
            object : JsonRpcTransport by fake {
                override suspend fun send(message: JsonRpcMessage) {
                    fake.send(message)
                    if (message is JsonRpcRequest && message.method == "thread/archive") events += "sent"
                }
            }
        }
        var callbackId: CodexRequestId? = null
        val pending = async {
            client.request(CodexRpc.Thread.Archive, ThreadArchiveParams(ThreadId("thr_1"))) {
                assertEquals(listOf("sent"), events)
                callbackId = it
                events += "callback"
            }.also { events += "returned" }
        }
        runCurrent()
        val request = transport.sent.last() as JsonRpcRequest
        assertEquals(CodexRequestId.Number((request.id as JsonRpcId.Number).value), callbackId)
        assertFalse(pending.isCompleted)
        transport.receive(JsonRpcResponse(request.id, json("{}")))
        assertEquals(CodexRpcUnit, pending.await())
        assertEquals(listOf("sent", "callback", "returned"), events)
    }

    @Test
    fun failedSendDoesNotInvokeTheRequestIdCallback() = runTest {
        val failure = IllegalStateException("Example transport failure")
        val (client, transport) = connect { fake ->
            object : JsonRpcTransport by fake {
                override suspend fun send(message: JsonRpcMessage) {
                    if (message is JsonRpcRequest && message.method == "thread/archive") throw failure
                    fake.send(message)
                }
            }
        }
        var callbackCalled = false
        val before = transport.sent.size
        val result = runCatching {
            client.request(CodexRpc.Thread.Archive, ThreadArchiveParams(ThreadId("thr_1"))) {
                callbackCalled = true
            }
        }
        assertSame(failure, result.exceptionOrNull())
        assertFalse(callbackCalled)
        assertEquals(before, transport.sent.size)
    }

    @Test
    fun callbackFailurePropagatesWithoutRetryAndLateResponseDoesNotAffectNextCall() = runTest {
        val (client, transport) = connect()
        val failure = IllegalArgumentException("Example callback failure")
        val before = transport.sent.size
        val result = runCatching {
            client.request(CodexRpc.Thread.Archive, ThreadArchiveParams(ThreadId("thr_1"))) { throw failure }
        }
        assertSame(failure, result.exceptionOrNull())
        assertEquals(before + 1, transport.sent.size)
        val first = transport.sent.last() as JsonRpcRequest
        val next = async { client.request(CodexRpc.Thread.Archive, ThreadArchiveParams(ThreadId("thr_2"))) }
        runCurrent()
        transport.receive(JsonRpcResponse(first.id, json("{}")))
        runCurrent()
        assertFalse(next.isCompleted)
        val second = transport.sent.last() as JsonRpcRequest
        transport.receive(JsonRpcResponse(second.id, json("{}")))
        assertEquals(CodexRpcUnit, next.await())
        assertEquals(before + 2, transport.sent.size)
    }

    @Test
    fun unitResultsDistinguishAbsentPayloadFromExplicitNull() = runTest {
        val (client, transport) = connect()
        for (payload in listOf(null, json("{}"), JsonNull)) {
            val pending = async { runCatching {
                client.request(CodexRpc.Thread.Archive, ThreadArchiveParams(ThreadId("thr_1")))
            } }
            runCurrent()
            val request = transport.sent.last() as JsonRpcRequest
            transport.receive(JsonRpcResponse(request.id, payload))
            val result = pending.await()
            if (payload == JsonNull) assertIs<SerializationException>(result.exceptionOrNull())
            else assertEquals(CodexRpcUnit, result.getOrThrow())
        }
    }

    @Test
    fun requiredResultsKeepMissingAndMalformedPayloadFailuresDistinct() = runTest {
        val (client, transport) = connect()
        for (payload in listOf(null, JsonNull, json("{}"), json("42"))) {
            val pending = async { runCatching {
                client.request(CodexRpc.Thread.Read, ThreadReadParams(ThreadId("thr_1")))
            } }
            runCurrent()
            val request = transport.sent.last() as JsonRpcRequest
            transport.receive(JsonRpcResponse(request.id, payload))
            val error = pending.await().exceptionOrNull()
            if (payload == null) assertIs<IllegalArgumentException>(error)
            else assertIs<SerializationException>(error)
        }
    }

    @Test
    fun malformedUnhandledRequestsRetainMethodSpecificDefaultsAndErrors() = runTest {
        val (client, transport) = connect()
        val defaults = mapOf(
            "item/commandExecution/requestApproval" to """{"decision":"decline"}""",
            "item/fileChange/requestApproval" to """{"decision":"decline"}""",
            "item/permissions/requestApproval" to """{"permissions":{}}""",
            "item/tool/requestUserInput" to """{"decision":"cancel"}""",
            "mcpServer/elicitation/request" to """{"action":"decline","content":null}""",
            "attestation/generate" to """{"status":"unsupported"}""",
            "item/tool/call" to null,
            "example/unknown" to null,
        )
        for ((method, expected) in defaults) {
            val observed = async { client.serverRequests.first() }
            runCurrent()
            val id = JsonRpcId.StringId(method)
            transport.receive(JsonRpcRequest(id, method, json("42")))
            runCurrent()
            assertIs<CodexServerRequest.Unsupported>(observed.await())
            val response = transport.sent.last() as JsonRpcResponse
            assertEquals(id, response.id)
            if (expected != null) {
                assertEquals(json(expected), response.result)
                assertEquals(null, response.error)
            } else {
                assertEquals(if (method == "item/tool/call") -32602 else -32601, response.error?.code)
                assertEquals(null, response.result)
            }
        }
    }

    private suspend fun TestScope.connect(
        experimental: Boolean = false,
        wrap: (FakeJsonRpcTransport) -> JsonRpcTransport = { it },
    ): Pair<CodexClient, FakeJsonRpcTransport> {
        val transport = FakeJsonRpcTransport()
        val pending = async { CodexClients.connect(CodexClientConnection(
            wrap(transport), ClientInfo("cokit_test", "CoKit Test", "0.1.0"), backgroundScope,
            InitializeCapabilities(experimentalApi = experimental),
        )) }
        runCurrent()
        val initialize = transport.sent.single() as JsonRpcRequest
        transport.receive(JsonRpcResponse(initialize.id, json("{}")))
        val client = pending.await()
        assertTrue(client.isInitialized)
        runCurrent()
        return client to transport
    }

    private fun json(value: String) = CodexProtocolJson.parseToJsonElement(value)
}
