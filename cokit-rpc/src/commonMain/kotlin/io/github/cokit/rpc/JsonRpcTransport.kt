package io.github.cokit.rpc

import io.github.cokit.protocol.JsonRpcMessage
import kotlinx.coroutines.flow.Flow

interface JsonRpcTransport : AutoCloseable {
    val incoming: Flow<JsonRpcMessage>

    suspend fun send(message: JsonRpcMessage)

    override fun close()
}
