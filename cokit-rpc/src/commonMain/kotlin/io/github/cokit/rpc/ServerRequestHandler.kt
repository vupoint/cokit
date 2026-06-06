package io.github.cokit.rpc

import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse

fun interface ServerRequestHandler {
    suspend fun handle(request: JsonRpcRequest): JsonRpcResponse
}
