package io.github.cokit.testing

import io.github.cokit.protocol.JsonRpcNotification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class FakeJsonRpcTransportTest {
    @Test
    fun recordsSentMessages() = runTest {
        val transport = FakeJsonRpcTransport()

        transport.send(JsonRpcNotification(method = "initialized"))

        assertEquals(JsonRpcNotification(method = "initialized"), transport.sent.single())
    }
}
