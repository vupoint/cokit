package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.protocol.JsonRpcNotification
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CodexClientsTest {
    @Test
    fun connectAdvertisesOpenAiFormElicitationCapability() = runTest {
        val transport = FakeJsonRpcTransport()
        val client = async {
            CodexClients.connect(
                CodexClientConnection(
                    transport = transport,
                    clientInfo = ClientInfo("cokit_test", "CoKit Test", "0.1.0"),
                    capabilities = InitializeCapabilities(mcpServerOpenaiFormElicitation = true),
                    scope = backgroundScope,
                ),
            )
        }
        runCurrent()

        val initialize = transport.sent.single() as JsonRpcRequest
        assertEquals(
            true,
            initialize.params
                ?.jsonObject
                ?.get("capabilities")
                ?.jsonObject
                ?.get("mcpServerOpenaiFormElicitation")
                ?.jsonPrimitive
                ?.content
                ?.toBooleanStrict(),
        )

        transport.receive(JsonRpcResponse(initialize.id, result = JsonObject(emptyMap())))
        client.await()
    }

    @Test
    fun connectInitializesThenSendsInitializedNotification() = runTest {
        val transport = FakeJsonRpcTransport()

        val client = async {
            CodexClients.connect(
                CodexClientConnection(
                    transport = transport,
                    clientInfo = ClientInfo("cokit_test", "CoKit Test", "0.1.0"),
                    scope = backgroundScope,
                ),
            )
        }
        runCurrent()

        assertTrue(transport.sent.first() is JsonRpcRequest)
        val initialize = transport.sent.single() as JsonRpcRequest
        assertEquals("initialize", initialize.method)
        assertTrue(initialize.params.toString().contains("cokit_test"))
        assertFalse(client.isCompleted)

        transport.receive(JsonRpcResponse(initialize.id, result = JsonObject(emptyMap())))

        client.await()
        assertEquals(JsonRpcNotification(method = "initialized"), transport.sent.last())
    }
}
