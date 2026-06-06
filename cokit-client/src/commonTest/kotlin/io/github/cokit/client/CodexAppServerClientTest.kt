package io.github.cokit.client

import io.github.cokit.protocol.JsonRpcNotification
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class CodexAppServerClientTest {
    @Test
    fun connectSendsInitializeThenInitialized() = runTest {
        val transport = FakeJsonRpcTransport()

        CodexAppServerClient.connect(
            transport = transport,
            clientInfo = ClientInfo("cokit_test", "CoKit Test", "0.1.0"),
            scope = backgroundScope,
        )

        assertTrue(transport.sent.first() is JsonRpcRequest)
        assertEquals("initialize", (transport.sent.first() as JsonRpcRequest).method)
        assertEquals(JsonRpcNotification(method = "initialized"), transport.sent.last())
    }
}
