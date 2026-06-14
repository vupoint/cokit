package io.github.cokit.client

import io.github.cokit.client.approvals.ApprovalDecision
import io.github.cokit.protocol.JsonRpcNotification
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import io.github.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CodexRpcClientTest {
    @Test
    fun typedMethodDescriptorSendsMethodAndDecodesResult() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val thread = async {
            fixture.client.request(
                CodexRpc.Thread.Start,
                ThreadStartParams(
                    cwd = CodexHostPath("/path/to/project"),
                    approvalPolicy = ApprovalPolicy.OnRequest,
                    sandbox = SandboxPolicy.WorkspaceWrite,
                ),
            )
        }
        runCurrent()

        val threadRequest = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("thread/start", threadRequest.method)
        assertEquals("/path/to/project", threadRequest.params?.jsonObject?.get("cwd")?.jsonPrimitive?.contentOrNull)
        assertEquals("on-request", threadRequest.params?.jsonObject?.get("approvalPolicy")?.jsonPrimitive?.contentOrNull)
        assertEquals("workspace-write", threadRequest.params?.jsonObject?.get("sandbox")?.jsonPrimitive?.contentOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                threadRequest.id,
                result = buildJsonObject {
                    put("thread", buildJsonObject { put("id", "thr_123") })
                },
            ),
        )

        assertEquals(ThreadId("thr_123"), thread.await().thread.id)

        val turn = async {
            fixture.client.request(
                CodexRpc.Turn.Start,
                TurnStartParams(
                    threadId = ThreadId("thr_123"),
                    input = listOf(TurnInput.Text("Run tests")),
                ),
            )
        }
        runCurrent()

        val turnRequest = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("turn/start", turnRequest.method)
        assertEquals("thr_123", turnRequest.params?.jsonObject?.get("threadId")?.jsonPrimitive?.contentOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                turnRequest.id,
                result = buildJsonObject {
                    put("turn", buildJsonObject {
                        put("id", "turn_123")
                        put("status", "running")
                    })
                },
            ),
        )

        assertEquals(TurnId("turn_123"), turn.await().turn.id)
    }

    @Test
    fun notificationsExposeTypedModelsWithoutJsonPayloads() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        val notification = async { fixture.client.notifications.first() }

        fixture.transport.receive(
            JsonRpcNotification(
                method = "thread/started",
                params = buildJsonObject {
                    put("threadId", "thr_123")
                },
            ),
        )
        runCurrent()

        assertEquals(
            CodexNotification.ThreadStarted(ThreadId("thr_123")),
            notification.await(),
        )
    }

    @Test
    fun commandApprovalServerRequestsAreTypedAndDeclineByDefault() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        val serverRequest = async { fixture.client.serverRequests.first() }

        fixture.transport.receive(commandApprovalRequest())
        runCurrent()

        val commandApproval = assertIs<CodexServerRequest.CommandApproval>(serverRequest.await())
        assertEquals("git status", commandApproval.request.command)
        assertEquals(CodexHostPath("/path/to/project"), commandApproval.request.cwd)

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertTrue(response.result.toString().contains("decline"))
    }

    @Test
    fun commandApprovalHandlerCanReturnTypedDecision() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        fixture.client.registerCommandApprovalHandler { request ->
            assertEquals("git status", request.command)
            ApprovalDecision.AcceptForSession
        }

        fixture.transport.receive(commandApprovalRequest())
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertTrue(response.result.toString().contains("accept_for_session"))
    }

    private suspend fun TestScope.connectedRpcClientFixture(
        scope: CoroutineScope,
    ): ConnectedRpcClientFixture {
        val transport = FakeJsonRpcTransport()
        val client = async {
            CodexRpcClient.connect(
                CodexRpcConnection(
                    transport = transport,
                    clientInfo = ClientInfo("cokit_test", "CoKit Test", "0.1.0"),
                    scope = scope,
                ),
            )
        }
        runCurrent()
        val initialize = transport.sent.single() as JsonRpcRequest
        transport.receive(JsonRpcResponse(initialize.id, result = JsonObject(emptyMap())))
        return ConnectedRpcClientFixture(client.await(), transport)
    }

    private fun commandApprovalRequest(): JsonRpcRequest = JsonRpcRequest(
        id = io.github.cokit.protocol.JsonRpcId.Number(99),
        method = "item/commandExecution/requestApproval",
        params = buildJsonObject {
            put("threadId", "thr_123")
            put("turnId", "turn_123")
            put("itemId", "item_123")
            put("command", "git status")
            put("cwd", "/path/to/project")
        },
    )

    private data class ConnectedRpcClientFixture(
        val client: CodexRpcClient,
        val transport: FakeJsonRpcTransport,
    )
}
