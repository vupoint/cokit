package io.github.cokit.testing

import io.github.cokit.protocol.JsonRpcMessage
import io.github.cokit.rpc.JsonRpcTransport
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class FakeJsonRpcTransport : JsonRpcTransport {
    private val mutableIncoming = MutableSharedFlow<JsonRpcMessage>()
    private val mutableSent = mutableListOf<JsonRpcMessage>()

    override val incoming: SharedFlow<JsonRpcMessage> = mutableIncoming

    val sent: List<JsonRpcMessage>
        get() = mutableSent.toList()

    override suspend fun send(message: JsonRpcMessage) {
        mutableSent += message
    }

    suspend fun receive(message: JsonRpcMessage) {
        mutableIncoming.emit(message)
    }

    override fun close() = Unit
}
