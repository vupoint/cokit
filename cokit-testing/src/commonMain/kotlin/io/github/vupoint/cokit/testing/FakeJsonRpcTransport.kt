package io.github.vupoint.cokit.testing

import io.github.vupoint.cokit.protocol.JsonRpcMessage
import io.github.vupoint.cokit.rpc.JsonRpcTransport
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * In-memory transport for tests that records sends and allows explicit incoming injection.
 *
 * No parsing, framing, message-size checks, or closed-state enforcement are performed.
 * Sent messages are retained without a bound; use bounded test transcripts and coordinate
 * access to the recorded list when using multiple coroutines or threads.
 */
class FakeJsonRpcTransport : JsonRpcTransport {
    private val mutableIncoming = MutableSharedFlow<JsonRpcMessage>()
    private val mutableSent = mutableListOf<JsonRpcMessage>()

    /**
     * Hot incoming stream with no replay or buffer. Injection waits for active subscribers
     * to receive the message; without subscribers, the message is immediately lost.
     * [close] does not complete the flow.
     */
    override val incoming: SharedFlow<JsonRpcMessage> = mutableIncoming

    /** Snapshot of all messages recorded by [send], in send order. */
    val sent: List<JsonRpcMessage>
        get() = mutableSent.toList()

    /** Records [message] without sending it to a peer, including after [close]. */
    override suspend fun send(message: JsonRpcMessage) {
        mutableSent += message
    }

    /** Injects [message] into [incoming] using its unbuffered shared-flow emission semantics. */
    suspend fun receive(message: JsonRpcMessage) {
        mutableIncoming.emit(message)
    }

    /** No-op; recordings and message injection remain available. */
    override fun close() = Unit
}
