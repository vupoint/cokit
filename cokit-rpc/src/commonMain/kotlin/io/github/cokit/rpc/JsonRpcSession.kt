package io.github.cokit.rpc

import io.github.cokit.protocol.JsonRpcId
import io.github.cokit.protocol.JsonRpcMessage
import io.github.cokit.protocol.JsonRpcNotification
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class JsonRpcSession(
    private val transport: JsonRpcTransport,
    private val scope: CoroutineScope,
) : AutoCloseable {
    private var nextRequestId = 1L
    private val mutableNotifications = MutableSharedFlow<JsonRpcNotification>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val notifications: SharedFlow<JsonRpcNotification> = mutableNotifications

    suspend fun notify(method: String) {
        transport.send(JsonRpcNotification(method = method))
    }

    suspend fun request(method: String): JsonRpcId {
        val id = JsonRpcId.Number(nextRequestId++)
        transport.send(JsonRpcRequest(id = id, method = method))
        return id
    }

    suspend fun sendResponse(response: JsonRpcResponse) {
        transport.send(response)
    }

    suspend fun publishForTests(message: JsonRpcMessage) {
        if (message is JsonRpcNotification) {
            mutableNotifications.emit(message)
        }
    }

    override fun close() {
        transport.close()
    }
}
