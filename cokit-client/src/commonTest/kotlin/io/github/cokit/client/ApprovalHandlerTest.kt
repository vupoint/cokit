package io.github.cokit.client

import io.github.cokit.protocol.JsonRpcId
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ApprovalHandlerTest {
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

    private suspend fun TestScope.connectedClientFixture(
        scope: CoroutineScope,
    ): ConnectedClientFixture {
        val transport = FakeJsonRpcTransport()
        val client = async {
            CodexAppServerClient.connect(
                transport = transport,
                clientInfo = ClientInfo("cokit_test", "CoKit Test", "0.1.0"),
                scope = scope,
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
