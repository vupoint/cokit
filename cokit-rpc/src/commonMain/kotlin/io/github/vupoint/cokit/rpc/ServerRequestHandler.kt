package io.github.vupoint.cokit.rpc

import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse

/**
 * Application-provided response logic for a server-initiated request.
 *
 * Implementations must return the request's identifier and explicitly decide whether
 * approval-like operations are permitted. This interface does not register a handler,
 * send its response, or supply an approval default.
 */
fun interface ServerRequestHandler {
    /** Produces a response for [request]; exceptions propagate to the caller invoking the handler. */
    suspend fun handle(request: JsonRpcRequest): JsonRpcResponse
}
