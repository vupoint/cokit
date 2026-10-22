package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.environment.EnvironmentAddParams
import io.github.vupoint.cokit.client.environment.EnvironmentId
import io.github.vupoint.cokit.client.environment.EnvironmentSkillsParams
import io.github.vupoint.cokit.protocol.CodexProtocolJson
import io.github.vupoint.cokit.protocol.JsonRpcErrorObject
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.rpc.JsonRpcRemoteException
import io.github.vupoint.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, ExperimentalCodexApi::class)
class Release163RpcTest {
    @Test
    fun attachmentOwnerLookupPreservesIdentityArchiveFilterAndPagination() = runTest {
        val fixture = connect()
        for (archived in listOf(null, false, true)) {
            val filter = archived?.let { "\"archived\":$it," } ?: ""
            val result = call(fixture, CodexRpc.ThreadAttachmentOwner.List,
                ThreadAttachmentOwnerListParams("pull_request", "example", archived, CodexCursor("page"), 1),
                "thread/attachmentOwner/list",
                """{"attachmentType":"pull_request","identityKey":"example",${filter}"cursor":"page","limit":1}""",
                """{"data":[{"threadId":"thr_1","archived":false}],"nextCursor":"next"}""")
            assertEquals(ThreadId("thr_1"), result.data.single().threadId)
            assertEquals(CodexCursor("next"), result.nextCursor)
        }
    }

    @Test
    fun facadesForwardExclusionsReadReceiptsAndTurnAncestry() = runTest {
        val fixture = connect(experimental = true)
        val listing = async { fixture.first.threads.list(ListThreadsRequest(excludedThreadIds = listOf(ThreadId("thr_hidden")))) }
        runCurrent()
        val listRequest = fixture.second.sent.last() as JsonRpcRequest
        assertEquals("thread/list", listRequest.method)
        assertEquals(json("""{"excludedThreadIds":["thr_hidden"]}"""), listRequest.params)
        fixture.second.receive(JsonRpcResponse(listRequest.id, json("""{"data":[{"id":"thr_1"}],"readStates":{"thr_1":{"revision":"rev_1"}}}""")))
        assertEquals(ThreadReadStateRevision("rev_1"), listing.await().readStates?.get(ThreadId("thr_1"))?.revision)

        val starting = async { fixture.first.turns.start(StartTurnRequest(ThreadId("thr_1"), parentTurnId = TurnId("turn_parent"), rootTurnId = TurnId("turn_root"))) }
        runCurrent()
        val turnRequest = fixture.second.sent.last() as JsonRpcRequest
        assertEquals("turn/start", turnRequest.method)
        assertEquals(json("""{"threadId":"thr_1","input":[],"parentTurnId":"turn_parent","rootTurnId":"turn_root"}"""), turnRequest.params)
        fixture.second.receive(JsonRpcResponse(turnRequest.id, json("""{"turn":{"id":"turn_new","status":"inProgress","rootTurnId":"turn_root"}}""")))
        assertEquals(TurnId("turn_root"), starting.await().rootTurnId)
    }

    @Test
    fun newExperimentalRequestsRejectBothOverloadsBeforeSending() = runTest {
        val fixture = connect()
        rejectWithoutOptIn(fixture, CodexRpc.Thread.ReadItems, ThreadItemsReadParams(ThreadId("thr_1"), TurnId("turn_1"), listOf(ItemId("item_1"))))
        rejectWithoutOptIn(fixture, CodexRpc.ThreadPrediction.Request, ThreadPredictionRequestParams(ThreadId("thr_1"), TurnId("turn_1")))
        rejectWithoutOptIn(fixture, CodexRpc.ThreadReadState.Update, ThreadReadStateUpdateParams(ThreadId("thr_1"), ThreadReadStateRevision("rev_1"), ThreadReadStateOperation.Read))
        rejectWithoutOptIn(fixture, CodexRpc.Bedrock.CheckGovCloudRequirements, CodexRpcUnit)
        rejectWithoutOptIn(fixture, CodexRpc.Environment.Add, EnvironmentAddParams(EnvironmentId("env_1"), "wss://example.invalid"))
        rejectWithoutOptIn(fixture, CodexRpc.Thread.Fork, ThreadForkParams(ThreadId("thr_1"), ephemeral = true, experimentalPredictionMode = ThreadPredictionMode(true)))
        rejectWithoutOptIn(fixture, CodexRpc.Thread.Fork, ThreadForkParams(ThreadId("thr_1"), experimentalPredictionMode = ThreadPredictionMode(false)))
        val before = fixture.second.sent.size
        assertFailsWith<IllegalArgumentException> {
            fixture.first.threads.fork(ForkThreadRequest(ThreadId("thr_1"), ephemeral = true, experimentalPredictionMode = ThreadPredictionMode(true)))
        }
        assertEquals(before, fixture.second.sent.size)
        call(fixture, CodexRpc.Thread.Fork, ThreadForkParams(ThreadId("thr_1")), "thread/fork",
            """{"threadId":"thr_1"}""", """{"thread":{"id":"thr_fork"}}""")
    }

    @Test
    fun optedInRequestsSendExactContractsAndPreserveResults() = runTest {
        val fixture = connect(experimental = true)
        val items = call(fixture, CodexRpc.Thread.ReadItems,
            ThreadItemsReadParams(ThreadId("thr_1"), TurnId("turn_1"), listOf(ItemId("item_1"))),
            "thread/items/read", """{"threadId":"thr_1","turnId":"turn_1","itemIds":["item_1"]}""",
            """{"data":[{"turnId":"turn_1","item":{"id":"item_1","type":"future"}}]}""")
        assertEquals("""{"id":"item_1","type":"future"}""", items.data.single().item.toJsonString())
        val updated = call(fixture, CodexRpc.ThreadReadState.Update,
            ThreadReadStateUpdateParams(ThreadId("thr_1"), ThreadReadStateRevision("rev_1"), ThreadReadStateOperation.Unread),
            "thread/readState/update", """{"threadId":"thr_1","expectedRevision":"rev_1","operation":{"type":"unread"}}""",
            """{"readState":{"revision":"rev_2","firstUnread":{"type":"threadStart"}}}""")
        assertEquals(ThreadUnreadPosition.ThreadStart, updated.readState.firstUnread)
        call(fixture, CodexRpc.ThreadPrediction.Request, ThreadPredictionRequestParams(ThreadId("thr_1"), TurnId("turn_1")),
            "thread/prediction/request", """{"threadId":"thr_1","sourceTurnId":"turn_1"}""", "{}")
        val advisory = call(fixture, CodexRpc.Bedrock.CheckGovCloudRequirements, CodexRpcUnit,
            "account/bedrock/checkGovCloudRequirements", "{}", """{"isGovCloud":true,"shouldWarn":true}""")
        assertEquals(true, advisory.shouldWarn)
        call(fixture, CodexRpc.Environment.Add,
            EnvironmentAddParams(EnvironmentId("env_1"), "wss://example.invalid", EnvironmentSkillsParams(listOf("example")), "request_1"),
            "environment/add", """{"environmentId":"env_1","execServerUrl":"wss://example.invalid","skills":{"required":["example"]},"websocketRequestId":"request_1"}""", "{}")
        val fork = async { fixture.first.threads.fork(ForkThreadRequest(ThreadId("thr_1"), ephemeral = true, experimentalPredictionMode = ThreadPredictionMode(true))) }
        runCurrent()
        val request = fixture.second.sent.last() as JsonRpcRequest
        assertEquals("thread/fork", request.method)
        assertEquals(json("""{"threadId":"thr_1","ephemeral":true,"experimentalPredictionMode":true}"""), request.params)
        fixture.second.receive(JsonRpcResponse(request.id, json("""{"thread":{"id":"thr_fork"}}""")))
        assertEquals(ThreadId("thr_fork"), fork.await().id)
    }

    @Test
    fun revisionConflictPreservesCurrentReceiptAndDoesNotRetry() = runTest {
        val fixture = connect(experimental = true)
        val pending = async { runCatching {
            fixture.first.request(CodexRpc.ThreadReadState.Update,
                ThreadReadStateUpdateParams(ThreadId("thr_1"), ThreadReadStateRevision("seen"), ThreadReadStateOperation.Read))
        } }
        runCurrent()
        val request = fixture.second.sent.last() as JsonRpcRequest
        val before = fixture.second.sent.size
        val data = json("""{"reason":"readStateConflict","readState":{"revision":"unseen","firstUnread":{"type":"turn","turnId":"turn_new"}}}""")
        fixture.second.receive(JsonRpcResponse(request.id, error = JsonRpcErrorObject(-32600, "Revision conflict", data)))
        val error = assertIs<JsonRpcRemoteException>(pending.await().exceptionOrNull())
        assertEquals(-32600, error.error.code)
        assertEquals(data, error.error.data)
        runCurrent()
        assertEquals(before, fixture.second.sent.size)
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
