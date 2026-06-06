package io.github.cokit.rpc

import io.github.cokit.protocol.JsonRpcId
import io.github.cokit.protocol.JsonRpcMessage
import io.github.cokit.protocol.JsonRpcNotification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.test.runTest

class JsonRpcSessionTest {
    @Test
    fun requestSendsIncrementingNumericIds() = runTest {
        val transport = FakeJsonRpcTransport()
        val session = JsonRpcSession(transport, backgroundScope)

        val first = session.request("model/list")
        val second = session.request("thread/list")

        assertEquals(JsonRpcId.Number(1), first)
        assertEquals(JsonRpcId.Number(2), second)
        assertEquals(2, transport.sent.size)
    }

    @Test
    fun notifySendsNotification() = runTest {
        val transport = FakeJsonRpcTransport()
        val session = JsonRpcSession(transport, backgroundScope)

        session.notify("initialized")

        assertEquals(JsonRpcNotification(method = "initialized"), transport.sent.single())
    }

    private class FakeJsonRpcTransport : JsonRpcTransport {
        private val mutableIncoming = MutableSharedFlow<JsonRpcMessage>()
        private val mutableSent = mutableListOf<JsonRpcMessage>()

        override val incoming: SharedFlow<JsonRpcMessage> = mutableIncoming

        val sent: List<JsonRpcMessage>
            get() = mutableSent.toList()

        override suspend fun send(message: JsonRpcMessage) {
            mutableSent += message
        }

        override fun close() = Unit
    }
}
