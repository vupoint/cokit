package io.github.cokit.client

import io.github.cokit.client.approvals.CommandApprovalRequest
import io.github.cokit.protocol.JsonRpcId
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import io.github.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ApprovalHandlerTest {
    @Test
    fun commandApprovalRequestUsesSdkValueTypes() {
        val request = CommandApprovalRequest(
            threadId = ThreadId("thr_123"),
            turnId = TurnId("turn_123"),
            itemId = "item_123",
            command = "git status",
            cwd = CodexHostPath("/path/to/project"),
        )

        assertEquals(ThreadId("thr_123"), request.threadId)
        assertEquals(TurnId("turn_123"), request.turnId)
        assertEquals(CodexHostPath("/path/to/project"), request.cwd)
    }

    @Test
    fun commandApprovalDeclinesWhenNoHandlerIsRegistered() = runTest {
        val fixture = connectedClientFixture(backgroundScope)

        fixture.transport.receive(
            JsonRpcRequest(
                id = JsonRpcId.Number(99),
                method = "item/commandExecution/requestApproval",
                params = buildJsonObject {
                    put("threadId", "thr_123")
                    put("turnId", "turn_123")
                    put("itemId", "item_123")
                    put("command", "git status")
                    put("cwd", "/tmp/project")
                },
            ),
        )
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertTrue(response.result.toString().contains("decline"))
    }

    @Test
    fun serverRequestHandlersUseSdkRequestAndResponseTypes() = runTest {
        val fixture = connectedClientFixture(backgroundScope)
        fixture.client.registerServerRequestHandler("custom/request") { request ->
            assertEquals("custom/request", request.method)
            assertEquals("""{"value":"kept"}""", request.params?.toJsonString())
            CodexServerResponse.Result(CodexJsonPayload.parse("""{"decision":"decline"}"""))
        }

        fixture.transport.receive(
            JsonRpcRequest(
                id = JsonRpcId.Number(99),
                method = "custom/request",
                params = buildJsonObject {
                    put("value", "kept")
                },
            ),
        )
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertTrue(response.result.toString().contains("decline"))
    }

    @Test
    fun eventsExposeTypedNotificationEnvelope() = runTest {
        val fixture = connectedClientFixture(backgroundScope)
        val event = async {
            fixture.client.events.first()
        }

        fixture.transport.receive(
            io.github.cokit.protocol.JsonRpcNotification(
                method = "thread/started",
                params = buildJsonObject {
                    put("threadId", "thr_123")
                },
            ),
        )
        runCurrent()

        val notification = event.await() as CodexEvent.Notification
        assertEquals("thread/started", notification.method)
        assertEquals("""{"threadId":"thr_123"}""", notification.params?.toJsonString())
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
