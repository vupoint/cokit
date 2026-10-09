package io.github.vupoint.cokit.rpc

import io.github.vupoint.cokit.protocol.JsonRpcMessage
import kotlinx.coroutines.flow.Flow

/**
 * Transport boundary for already decoded JSON-RPC envelopes.
 *
 * Implementations define framing, buffering, error propagation, and resource ownership.
 * A [JsonRpcSession] closes the transport passed to it when the session is closed.
 */
interface JsonRpcTransport : AutoCloseable {
    /** Incoming peer messages; replay, completion, and loss behavior depend on the implementation. */
    val incoming: Flow<JsonRpcMessage>

    /** Sends one envelope, propagating transport-specific write failures to the caller. */
    suspend fun send(message: JsonRpcMessage)

    /** Releases the transport's resources according to the implementation's ownership rules. */
    override fun close()
}
