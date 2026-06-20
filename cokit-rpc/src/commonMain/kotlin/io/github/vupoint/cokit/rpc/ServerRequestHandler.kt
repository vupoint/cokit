package io.github.vupoint.cokit.rpc

import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse

fun interface ServerRequestHandler {
    suspend fun handle(request: JsonRpcRequest): JsonRpcResponse
}
