package io.github.cokit.transport.websocket

import kotlin.test.Test
import kotlin.test.assertEquals

class WebSocketCodexTransportTest {
    @OptIn(ExperimentalCodexWebSocketTransport::class)
    @Test
    fun storesTargetUrl() {
        val transport = WebSocketCodexTransport("ws://127.0.0.1:12345")

        assertEquals("ws://127.0.0.1:12345", transport.url)
    }
}
