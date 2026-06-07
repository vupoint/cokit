package io.github.cokit.client

import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import io.github.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ThreadTurnApiTest {
    @Test
    fun startThreadSendsThreadStartAndReturnsThread() = runTest {
        val fixture = connectedClientFixture(backgroundScope)

        val deferred = async {
            fixture.client.threads.start(
                StartThreadRequest(
                    cwd = CodexHostPath("/path/to/project"),
                    approvalPolicy = ApprovalPolicy.OnRequest,
                    sandbox = SandboxPolicy.WorkspaceWrite,
                    model = ModelName("gpt-5"),
                    effort = ReasoningEffort.Medium,
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
        assertEquals("medium", params["effort"]?.jsonPrimitive?.contentOrNull)

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
                    input = listOf(buildJsonObject { put("text", "Run tests") }),
                    approvalPolicy = ApprovalPolicy.OnFailure,
                    sandboxPolicy = SandboxPolicy.WorkspaceWrite,
                    outputSchema = buildJsonObject {
                        put("type", "object")
                    },
                ),
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("turn/start", request.method)
        val params = request.params!!.jsonObject
        assertEquals("thr_123", params["threadId"]?.jsonPrimitive?.contentOrNull)
        assertEquals("on-failure", params["approvalPolicy"]?.jsonPrimitive?.contentOrNull)
        assertEquals("workspace-write", params["sandboxPolicy"]?.jsonPrimitive?.contentOrNull)
        assertTrue(params.containsKey("input"))
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

    private suspend fun TestScope.connectedClientFixture(
        scope: CoroutineScope,
    ): ConnectedClientFixture {
        val transport = FakeJsonRpcTransport()
        val client = async {
            CodexAppServerClient.connect(
                CodexClientOptions(
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
        val client: CodexAppServerClient,
        val transport: FakeJsonRpcTransport,
    )
}
