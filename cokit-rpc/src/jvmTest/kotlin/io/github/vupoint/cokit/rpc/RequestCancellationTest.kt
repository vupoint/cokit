package io.github.vupoint.cokit.rpc

import io.github.vupoint.cokit.protocol.JsonRpcMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RequestCancellationTest {
    @Test
    fun cancellationRemovesPendingCorrelationWithoutAResponse() = runTest {
        val transport = object : JsonRpcTransport {
            override val incoming = MutableSharedFlow<JsonRpcMessage>()
            override suspend fun send(message: JsonRpcMessage) = Unit
            override fun close() = Unit
        }
        val session = JsonRpcSession(transport, backgroundScope)
        // Inspect retained state without introducing a production testing API.
        val field = JsonRpcSession::class.java.getDeclaredField("pendingRequests").apply { isAccessible = true }
        val pending = field.get(session) as Map<*, *>
        val request = async { session.request("example/wait") }
        runCurrent()
        assertEquals(1, pending.size)

        request.cancelAndJoin()

        assertEquals(0, pending.size)
        session.close()
    }
}
