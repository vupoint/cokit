package io.github.cokit.client

import io.github.cokit.client.attestation.AttestationGenerateRequest
import io.github.cokit.client.attestation.AttestationGenerateResponse
import io.github.cokit.protocol.JsonRpcId
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import io.github.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AttestationRequestTest {
    @Test
    fun attestationGenerateServerRequestsAreTypedAndUnsupportedByDefault() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        val serverRequest = async { fixture.client.serverRequests.first() }

        fixture.transport.receive(attestationGenerateRequest(id = 99))
        runCurrent()

        val attestation = assertIs<CodexServerRequest.AttestationGenerate>(serverRequest.await())
        assertEquals(AttestationGenerateRequest, attestation.request)

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals("unsupported", response.result?.jsonObject?.get("status")?.jsonPrimitive?.content)
    }

    @Test
    fun attestationGenerateHandlerCanReturnOpaqueToken() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        fixture.client.registerAttestationGenerateHandler { request ->
            assertEquals(AttestationGenerateRequest, request)
            AttestationGenerateResponse(token = "v1.opaque-client-token")
        }

        fixture.transport.receive(attestationGenerateRequest(id = 99))
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals("v1.opaque-client-token", response.result?.jsonObject?.get("token")?.jsonPrimitive?.content)
    }

    @Test
    fun malformedAttestationGenerateParamsReturnInvalidParamsWithoutCallingTypedHandler() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        var handlerCalled = false
        fixture.client.registerAttestationGenerateHandler {
            handlerCalled = true
            AttestationGenerateResponse(token = "v1.should-not-be-used")
        }

        fixture.transport.receive(
            JsonRpcRequest(
                id = JsonRpcId.Number(99),
                method = "attestation/generate",
            ),
        )
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals(-32602, response.error?.code)
        assertEquals("Invalid params for attestation/generate", response.error?.message)
        assertFalse(handlerCalled)
    }

    @Test
    fun attestationGenerateHandlerExceptionsReturnGenericJsonRpcError() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        fixture.client.registerAttestationGenerateHandler {
            error("private attestation token source should not leak")
        }

        fixture.transport.receive(attestationGenerateRequest(id = 99))
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals(-32000, response.error?.code)
        assertEquals("Server request handler failed", response.error?.message)
        assertFalse(response.toString().contains("private attestation token source"))
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

    private fun attestationGenerateRequest(id: Long): JsonRpcRequest = JsonRpcRequest(
        id = JsonRpcId.Number(id),
        method = "attestation/generate",
        params = buildJsonObject {},
    )

    private data class ConnectedRpcClientFixture(
        val client: CodexRpcClient,
        val transport: FakeJsonRpcTransport,
    )
}
