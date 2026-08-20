package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ThreadTurnApiTest {
    @Test
    fun listLoadedReturnsTypedThreadIdsAndNextCursor() = runTest {
        val fixture = connectedClientFixture(backgroundScope)
        val deferred = async {
            fixture.client.threads.listLoaded(
                ListLoadedThreadsRequest(
                    cursor = CodexCursor("cursor_123"),
                    limit = 2,
                ),
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("thread/loaded/list", request.method)
        assertEquals("cursor_123", request.params?.jsonObject?.get("cursor")?.jsonPrimitive?.contentOrNull)
        assertEquals(2, request.params?.jsonObject?.get("limit")?.jsonPrimitive?.content?.toInt())
        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put("data", kotlinx.serialization.json.buildJsonArray {
                        add("thr_1")
                        add("thr_2")
                    })
                    put("nextCursor", "cursor_456")
                },
            ),
        )

        assertEquals(listOf(ThreadId("thr_1"), ThreadId("thr_2")), deferred.await().threadIds)
        assertEquals(CodexCursor("cursor_456"), deferred.await().nextCursor)
    }

    @Test
    fun unarchiveReturnsRefreshedThread() = runTest {
        val fixture = connectedClientFixture(backgroundScope)
        val deferred = async<Any> {
            fixture.client.threads.unarchive(ThreadId("thr_123"))
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put("thread", buildJsonObject { put("id", "thr_123") })
                },
            ),
        )

        assertEquals(ThreadId("thr_123"), assertIs<Thread>(deferred.await()).id)
    }

    @Test
    fun steerReturnsAcceptedTurnId() = runTest {
        val fixture = connectedClientFixture(backgroundScope)
        val deferred = async<Any> {
            fixture.client.turns.steer(
                SteerTurnRequest(
                    threadId = ThreadId("thr_123"),
                    expectedTurnId = TurnId("turn_123"),
                    input = listOf(TurnInput.Text("Keep going")),
                ),
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject { put("turnId", "turn_123") },
            ),
        )

        assertEquals(TurnId("turn_123"), assertIs<TurnId>(deferred.await()))
    }

    @Test
    fun startThreadSendsThreadStartAndReturnsThread() = runTest {
        val fixture = connectedClientFixture(backgroundScope)

        val deferred = async {
            fixture.client.threads.start(
                StartThreadRequest(
                    cwd = CodexHostPath("/path/to/project"),
                    approvalPolicy = ApprovalPolicy.OnRequest,
                    sandbox = SandboxMode.WorkspaceWrite,
                    model = ModelName("gpt-5"),
                    serviceTier = ServiceTier("fast"),
                ),
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("thread/start", request.method)
        val params = request.params!!.jsonObject
        assertEquals("/path/to/project", params["cwd"]?.jsonPrimitive?.contentOrNull)
        assertEquals("on-request", params["approvalPolicy"]?.jsonPrimitive?.contentOrNull)
        assertEquals("workspace-write", params["sandbox"]?.jsonPrimitive?.contentOrNull)
        assertEquals("gpt-5", params["model"]?.jsonPrimitive?.contentOrNull)
        assertEquals("fast", params["serviceTier"]?.jsonPrimitive?.contentOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put("thread", buildJsonObject { put("id", "thr_123") })
                },
            ),
        )

        assertEquals(ThreadId("thr_123"), deferred.await().id)
    }

    @Test
    fun startTurnSendsTurnStartAndReturnsTurn() = runTest {
        val fixture = connectedClientFixture(backgroundScope)

        val deferred = async {
            fixture.client.turns.start(
                StartTurnRequest(
                    threadId = ThreadId("thr_123"),
                    input = listOf(TurnInput.Text("Run tests")),
                    approvalPolicy = ApprovalPolicy.OnRequest,
                    sandboxPolicy = SandboxPolicy.WorkspaceWrite(),
                    outputSchema = CodexJsonPayload.parse("""{"type":"object"}"""),
                ),
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("turn/start", request.method)
        val params = request.params!!.jsonObject
        assertEquals("thr_123", params["threadId"]?.jsonPrimitive?.contentOrNull)
        assertEquals("on-request", params["approvalPolicy"]?.jsonPrimitive?.contentOrNull)
        assertEquals(
            "workspaceWrite",
            params["sandboxPolicy"]?.jsonObject?.get("type")?.jsonPrimitive?.contentOrNull,
        )
        assertTrue(params.containsKey("input"))
        val inputItem = params["input"]
            ?.jsonArray
            ?.first()
            ?.jsonObject
        assertEquals("text", inputItem?.get("type")?.jsonPrimitive?.contentOrNull)
        assertEquals("Run tests", inputItem?.get("text")?.jsonPrimitive?.contentOrNull)
        assertEquals("object", params["outputSchema"]?.jsonObject?.get("type")?.jsonPrimitive?.contentOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put("turn", buildJsonObject {
                        put("id", "turn_123")
                        put("status", "running")
                    })
                },
            ),
        )

        assertEquals(TurnId("turn_123"), deferred.await().id)
    }

    @Test
    fun customTurnInputRequiresExplicitEscapeHatch() = runTest {
        val fixture = connectedClientFixture(backgroundScope)

        val deferred = async {
            fixture.client.turns.start(
                StartTurnRequest(
                    threadId = ThreadId("thr_123"),
                    input = listOf(
                        TurnInput.Custom(
                            CodexJsonPayload.parse("""{"type":"experimentalInput","value":"kept"}"""),
                        ),
                    ),
                ),
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        val inputItem = request.params!!
            .jsonObject["input"]!!
            .jsonArray
            .single()
            .jsonObject

        assertEquals("experimentalInput", inputItem["type"]?.jsonPrimitive?.contentOrNull)
        assertEquals("kept", inputItem["value"]?.jsonPrimitive?.contentOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put("turn", buildJsonObject {
                        put("id", "turn_123")
                        put("status", "running")
                    })
                },
            ),
        )

        assertEquals(TurnId("turn_123"), deferred.await().id)
    }

    @Test
    fun clientModelsUseSdkPayloadWrappersForUnmodeledProtocolMembers() {
        val payload = CodexJsonPayload.parse("""{"value":"kept"}""")

        val turn = Turn(
            id = TurnId("turn_123"),
            status = TurnStatus("running"),
            items = listOf(payload),
            error = TurnError("failed"),
        )
        val startThread = StartThreadRequest(config = payload)
        val resumeThread = ResumeThreadRequest(
            threadId = ThreadId("thr_123"),
            config = payload,
        )
        val startTurn = StartTurnRequest(
            threadId = ThreadId("thr_123"),
            outputSchema = payload,
        )

        assertEquals(payload, turn.items.single())
        assertEquals("failed", turn.error?.message)
        assertEquals(payload, startThread.config)
        assertEquals(payload, resumeThread.config)
        assertEquals(payload, startTurn.outputSchema)
    }

    private suspend fun TestScope.connectedClientFixture(
        scope: CoroutineScope,
    ): ConnectedClientFixture {
        val transport = FakeJsonRpcTransport()
        val client = async {
            CodexClients.connect(
                CodexClientConnection(
                    transport = transport,
                    clientInfo = ClientInfo("cokit_test", "CoKit Test", "0.1.0"),
                    scope = scope,
                ),
            )
        }
        runCurrent()
        val initialize = transport.sent.single() as JsonRpcRequest
        transport.receive(JsonRpcResponse(initialize.id, result = JsonObject(emptyMap())))
        return ConnectedClientFixture(client.await(), transport)
    }

    private data class ConnectedClientFixture(
        val client: CodexClient,
        val transport: FakeJsonRpcTransport,
    )
}
