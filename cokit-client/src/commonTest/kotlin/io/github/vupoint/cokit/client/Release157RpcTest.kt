package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.protocol.CodexProtocolJson
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class Release157RpcTest {
    @Test
    fun attachmentMethodsUseExplicitWireContracts() = runTest {
        val fixture = connect()
        val attachment = """{"id":"att_1","attachmentType":"pull_request","identityKey":"example","payload":{"future":true},"createdAt":1}"""
        val added = call(fixture, CodexRpc.ThreadAttachment.Add,
            ThreadAttachmentAddParams(ThreadId("thr_1"), "pull_request", "example", CodexJsonPayload.parse("""{"future":true}""")),
            """{"threadId":"thr_1","attachmentType":"pull_request","identityKey":"example","payload":{"future":true}}""",
            """{"attachment":$attachment,"outcome":"existing"}""")
        assertEquals(ThreadAttachmentAddOutcome.Existing, added.outcome)
        val listed = call(fixture, CodexRpc.ThreadAttachment.List, ThreadAttachmentListParams(ThreadId("thr_1"), limit = 1),
            """{"threadId":"thr_1","limit":1}""", """{"data":[$attachment],"nextCursor":"next"}""")
        assertEquals(CodexCursor("next"), listed.nextCursor)
        call(fixture, CodexRpc.ThreadAttachment.Remove, ThreadAttachmentRemoveParams(ThreadId("thr_1"), "pull_request", "example"),
            """{"threadId":"thr_1","attachmentType":"pull_request","identityKey":"example"}""", "{}")
    }

    @Test
    fun gatewayMethodsSendNoParamsAndRedactSensitiveDiagnostics() = runTest {
        val fixture = connect()
        val read = call(fixture, CodexRpc.GatewayOAuth.Read, CodexRpcUnit, null,
            """{"providerId":"example","providerName":"Example","required":true,"status":"notReady","error":"private-diagnostic"}""")
        assertEquals(true, read.required)
        assertFalse(read.toString().contains("private-diagnostic"))
        call(fixture, CodexRpc.GatewayOAuth.Login, CodexRpcUnit, null, "{}")
        call(fixture, CodexRpc.GatewayOAuth.Cancel, CodexRpcUnit, null, "{}")
        val event = CodexProtocolJson.decodeFromString(CodexNotification.GatewayOAuthChanged.serializer(),
            """{"providerId":"example","status":"started","authUrl":"https://example.invalid/private-auth","error":"private-diagnostic"}""")
        assertFalse(event.toString().contains("private-auth"))
        assertFalse(event.toString().contains("private-diagnostic"))
    }

    private suspend fun TestScope.connect(experimental: Boolean = false): Pair<CodexClient, FakeJsonRpcTransport> {
        val transport = FakeJsonRpcTransport()
        val pending = async { CodexClients.connect(CodexClientConnection(
            transport, ClientInfo("cokit_test", "CoKit Test", "0.1.0"), backgroundScope,
            InitializeCapabilities(experimentalApi = experimental),
        )) }
        runCurrent()
        val initialize = transport.sent.single() as JsonRpcRequest
        transport.receive(JsonRpcResponse(initialize.id, CodexProtocolJson.parseToJsonElement("{}")))
        return pending.await() to transport
    }

    private suspend fun <P : Any, R : Any> TestScope.call(
        fixture: Pair<CodexClient, FakeJsonRpcTransport>, method: CodexRpcMethod<P, R>, params: P,
        expectedParams: String?, result: String,
    ): R {
        val pending = async { fixture.first.request(method, params) }
        runCurrent()
        val request = fixture.second.sent.last() as JsonRpcRequest
        assertEquals(method.method, request.method)
        assertEquals(expectedParams?.let(CodexProtocolJson::parseToJsonElement), request.params)
        fixture.second.receive(JsonRpcResponse(request.id, CodexProtocolJson.parseToJsonElement(result)))
        return pending.await()
    }
}
