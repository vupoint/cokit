package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.protocol.CodexProtocolJson
import io.github.vupoint.cokit.protocol.JsonRpcErrorObject
import io.github.vupoint.cokit.rpc.JsonRpcRemoteException
import kotlin.test.assertIs
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, ExperimentalCodexApi::class)
class Release165RpcTest {
    @Test
    fun experimentalRequestsAndFieldsRejectBothOverloadsBeforeSending() = runTest {
        val fixture = connect()
        rejectWithoutOptIn(fixture, CodexRpc.Thread.Sample, ThreadSampleParams(ThreadId("thr_1"), TurnId("turn_1"), "Example"))
        rejectWithoutOptIn(fixture, CodexRpc.Thread.CancelSample, ThreadSampleCancelParams(CodexRequestId.Number(1)))
        rejectWithoutOptIn(fixture, CodexRpc.Thread.ReadMemory, ThreadMemoryReadParams(ThreadId("thr_1")))
        for (enabled in listOf(false, true)) {
            rejectWithoutOptIn(fixture, CodexRpc.Thread.Fork, ThreadForkParams(ThreadId("thr_1"), attachToSource = ThreadAttachmentMode(enabled)))
            rejectWithoutOptIn(fixture, CodexRpc.Thread.Fork, ThreadForkParams(ThreadId("thr_1"), experimentalDisablePredictionMcp = ThreadPredictionMcpDisabled(enabled)))
        }
        rejectWithoutOptIn(fixture, CodexRpc.Thread.List, ThreadListParams(attachedToThreadId = ThreadAttachedTo(ThreadId("thr_1"))))
        rejectWithoutOptIn(fixture, CodexRpc.Turn.Start, TurnStartParams(ThreadId("thr_1"), responsesapiClientMetadataPatch = TurnMetadataPatch(emptyMap())))
        rejectWithoutOptIn(fixture, CodexRpc.Turn.Steer, TurnSteerParams(ThreadId("thr_1"), TurnId("turn_1"), emptyList(), responsesapiClientMetadataPatch = TurnMetadataPatch(emptyMap())))
        val before = fixture.second.sent.size
        assertFailsWith<IllegalArgumentException> { fixture.first.threads.fork(ForkThreadRequest(ThreadId("thr_1"), attachToSource = ThreadAttachmentMode(false))) }
        assertFailsWith<IllegalArgumentException> { fixture.first.threads.list(ListThreadsRequest(attachedToThreadId = ThreadAttachedTo(ThreadId("thr_1")))) }
        assertFailsWith<IllegalArgumentException> { fixture.first.turns.start(StartTurnRequest(ThreadId("thr_1"), responsesapiClientMetadataPatch = TurnMetadataPatch(emptyMap()))) }
        assertFailsWith<IllegalArgumentException> { fixture.first.turns.steer(SteerTurnRequest(ThreadId("thr_1"), TurnId("turn_1"), emptyList(), responsesapiClientMetadataPatch = TurnMetadataPatch(emptyMap()))) }
        assertEquals(before, fixture.second.sent.size)
    }

    @Test
    fun feedbackSaveIsStableAndDoesNotRetryOrUploadAutomatically() = runTest {
        val fixture = connect()
        val saved = call(fixture, CodexRpc.Feedback.Save,
            FeedbackSaveParams("example", CodexHostPath("/path/to/feedback.zip"), includeLogs = true, extraLogFiles = listOf(CodexHostPath("/path/to/example.log")), tags = mapOf("example" to "value")),
            "feedback/save", """{"classification":"example","savePath":"/path/to/feedback.zip","includeLogs":true,"extraLogFiles":["/path/to/example.log"],"tags":{"example":"value"}}""",
            """{"threadId":"thr_1","savedPath":"/path/to/feedback.zip","promptHash":"example-hash"}""")
        assertEquals(CodexHostPath("/path/to/feedback.zip"), saved.savedPath)
        assertEquals("example-hash", saved.promptHash)
        assertEquals(3, fixture.second.sent.size)
    }

    @Test
    fun sampleKeepsRequestIdentityForExplicitCancellationAndNullableResults() = runTest {
        val fixture = connect(experimental = true)
        var id: CodexRequestId? = null
        val sample = async { fixture.first.request(CodexRpc.Thread.Sample,
            ThreadSampleParams(ThreadId("thr_1"), TurnId("turn_1"), "Example", disableMcp = true)) { id = it } }
        runCurrent()
        val request = fixture.second.sent.last() as JsonRpcRequest
        assertEquals("thread/sample", request.method)
        assertEquals(json("""{"threadId":"thr_1","expectedTurnId":"turn_1","prompt":"Example","disableMcp":true}"""), request.params)
        val target = requireNotNull(id)
        val encodedId = CodexProtocolJson.encodeToString(CodexRequestId.serializer(), target)
        call(fixture, CodexRpc.Thread.CancelSample, ThreadSampleCancelParams(target), "thread/sample/cancel", """{"requestId":$encodedId}""", "{}")
        fixture.second.receive(JsonRpcResponse(request.id, json("""{"sourceTurnId":"turn_1","responseId":"response_1","text":null,"inputTokens":null,"cachedInputTokens":null,"outputTokens":null}""")))
        assertEquals(null, sample.await().text)
        val memory = call(fixture, CodexRpc.Thread.ReadMemory, ThreadMemoryReadParams(ThreadId("thr_1")), "thread/memory/read", """{"threadId":"thr_1"}""", """{"summary":null}""")
        assertEquals(null, memory.summary)
        for (targetId in listOf(CodexRequestId.Number(12), CodexRequestId.StringId("sample_1"))) {
            val encoded = CodexProtocolJson.encodeToString(CodexRequestId.serializer(), targetId)
            call(fixture, CodexRpc.Thread.CancelSample, ThreadSampleCancelParams(targetId), "thread/sample/cancel", """{"requestId":$encoded}""", "{}")
        }
    }

    @Test
    fun facadesForwardAttachedForkFiltersAndMetadataPatches() = runTest {
        val fixture = connect(experimental = true)
        val fork = async { fixture.first.threads.fork(ForkThreadRequest(ThreadId("thr_1"), attachToSource = ThreadAttachmentMode(true), experimentalDisablePredictionMcp = ThreadPredictionMcpDisabled(false))) }
        runCurrent()
        var request = fixture.second.sent.last() as JsonRpcRequest
        assertEquals(json("""{"threadId":"thr_1","attachToSource":true,"experimentalDisablePredictionMcp":false}"""), request.params)
        fixture.second.receive(JsonRpcResponse(request.id, json("""{"thread":{"id":"thr_fork"}}""")))
        assertEquals(ThreadId("thr_fork"), fork.await().id)
        val listing = async { fixture.first.threads.list(ListThreadsRequest(attachedToThreadId = ThreadAttachedTo(ThreadId("thr_1")))) }
        runCurrent()
        request = fixture.second.sent.last() as JsonRpcRequest
        assertEquals(json("""{"attachedToThreadId":"thr_1"}"""), request.params)
        fixture.second.receive(JsonRpcResponse(request.id, json("""{"data":[]}""")))
        assertEquals(emptyList(), listing.await().threads)
        val start = async { fixture.first.turns.start(StartTurnRequest(ThreadId("thr_1"), responsesapiClientMetadataPatch = TurnMetadataPatch(mapOf("example" to "value")))) }
        runCurrent()
        request = fixture.second.sent.last() as JsonRpcRequest
        assertEquals(json("""{"threadId":"thr_1","input":[],"responsesapiClientMetadataPatch":{"example":"value"}}"""), request.params)
        fixture.second.receive(JsonRpcResponse(request.id, json("""{"turn":{"id":"turn_1","status":"inProgress"}}""")))
        assertEquals(TurnId("turn_1"), start.await().id)
        val steer = async { fixture.first.turns.steer(SteerTurnRequest(ThreadId("thr_1"), TurnId("turn_1"), emptyList(), responsesapiClientMetadataPatch = TurnMetadataPatch(emptyMap()))) }
        runCurrent()
        request = fixture.second.sent.last() as JsonRpcRequest
        assertEquals(json("""{"threadId":"thr_1","expectedTurnId":"turn_1","input":[],"responsesapiClientMetadataPatch":{}}"""), request.params)
        fixture.second.receive(JsonRpcResponse(request.id, json("""{"turnId":"turn_1"}""")))
        assertEquals(TurnId("turn_1"), steer.await())
    }

    @Test
    fun serverValidationFailuresKeepErrorDataAndAreNeverReplayed() = runTest {
        val fixture = connect(experimental = true)
        for (method in listOf(CodexRpc.Feedback.Save, CodexRpc.Thread.Sample)) {
            val pending = async { runCatching {
                if (method == CodexRpc.Feedback.Save) fixture.first.request(CodexRpc.Feedback.Save, FeedbackSaveParams("example", CodexHostPath("relative.zip")))
                else fixture.first.request(CodexRpc.Thread.Sample, ThreadSampleParams(ThreadId("thr_1"), TurnId("stale"), "Example"))
            } }
            runCurrent()
            val request = fixture.second.sent.last() as JsonRpcRequest
            val count = fixture.second.sent.size
            val data = json("""{"reason":"example_validation_failure"}""")
            fixture.second.receive(JsonRpcResponse(request.id, error = JsonRpcErrorObject(-32600, "Example rejection", data)))
            val failure = assertIs<JsonRpcRemoteException>(pending.await().exceptionOrNull())
            assertEquals(-32600, failure.error.code)
            assertEquals(data, failure.error.data)
            runCurrent()
            assertEquals(count, fixture.second.sent.size)
        }
    }

    private suspend fun <P : Any, R : Any> rejectWithoutOptIn(
        fixture: Pair<CodexClient, FakeJsonRpcTransport>, method: CodexRpcMethod<P, R>, params: P,
    ) {
        val before = fixture.second.sent.size
        var callbackCalled = false
        assertFailsWith<IllegalArgumentException> { fixture.first.request(method, params) }
        assertFailsWith<IllegalArgumentException> { fixture.first.request(method, params) { callbackCalled = true } }
        assertFalse(callbackCalled)
        assertEquals(before, fixture.second.sent.size)
    }

    private suspend fun TestScope.connect(experimental: Boolean = false): Pair<CodexClient, FakeJsonRpcTransport> {
        val transport = FakeJsonRpcTransport()
        val pending = async { CodexClients.connect(CodexClientConnection(
            transport, ClientInfo("cokit_test", "CoKit Test", "0.1.0"), backgroundScope,
            InitializeCapabilities(experimentalApi = experimental),
        )) }
        runCurrent()
        val initialize = transport.sent.single() as JsonRpcRequest
        transport.receive(JsonRpcResponse(initialize.id, json("{}")))
        return pending.await() to transport
    }

    private suspend fun <P : Any, R : Any> TestScope.call(
        fixture: Pair<CodexClient, FakeJsonRpcTransport>, method: CodexRpcMethod<P, R>, params: P,
        expectedMethod: String, expectedParams: String, result: String,
    ): R {
        val pending = async { fixture.first.request(method, params) }
        runCurrent()
        val request = fixture.second.sent.last() as JsonRpcRequest
        assertEquals(expectedMethod, request.method)
        assertEquals(json(expectedParams), request.params)
        fixture.second.receive(JsonRpcResponse(request.id, json(result)))
        return pending.await()
    }

    private fun json(value: String) = CodexProtocolJson.parseToJsonElement(value)
}
