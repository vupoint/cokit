package io.github.vupoint.cokit.transport.websocket

import io.github.vupoint.cokit.protocol.JsonRpcMessage
import io.github.vupoint.cokit.rpc.JsonRpcTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Compile-time opt-in for the experimental app-server WebSocket transport surface.
 * This opt-in does not enable an app-server connection or negotiate experimental protocol APIs.
 */
@RequiresOptIn("The Codex app-server WebSocket transport is experimental upstream.")
annotation class ExperimentalCodexWebSocketTransport

/**
 * Experimental placeholder for a WebSocket transport; networking and framing are not implemented.
 *
 * Construction only stores [url]. There is no URL validation, connection, authentication,
 * or server experimental-API negotiation. A future implementation requires a trusted local
 * endpoint or separately established authentication and transport security before use.
 *
 * @property url Intended endpoint, retained verbatim without validation or connection.
 */
@ExperimentalCodexWebSocketTransport
class WebSocketCodexTransport(
    val url: String,
) : JsonRpcTransport {
    /** Empty flow that completes immediately; no WebSocket messages are received. */
    override val incoming: Flow<JsonRpcMessage> = emptyFlow()

    /**
     * Always fails because WebSocket framing is not implemented.
     *
     * @throws IllegalStateException On every invocation.
     */
    override suspend fun send(message: JsonRpcMessage) {
        error("WebSocket transport framing is not implemented yet")
    }

    /** No-op because this placeholder owns no connection or other resources. */
    override fun close() = Unit
}
