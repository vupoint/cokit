package io.github.vupoint.cokit.transport.websocket

import io.github.vupoint.cokit.protocol.JsonRpcMessage
import io.github.vupoint.cokit.rpc.JsonRpcTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@RequiresOptIn("The Codex app-server WebSocket transport is experimental upstream.")
annotation class ExperimentalCodexWebSocketTransport

@ExperimentalCodexWebSocketTransport
class WebSocketCodexTransport(
    val url: String,
) : JsonRpcTransport {
    override val incoming: Flow<JsonRpcMessage> = emptyFlow()

    override suspend fun send(message: JsonRpcMessage) {
        error("WebSocket transport framing is not implemented yet")
    }

    override fun close() = Unit
}
