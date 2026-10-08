package io.github.vupoint.cokit.rpc

import io.github.vupoint.cokit.protocol.CodexProtocolJson
import io.github.vupoint.cokit.protocol.JsonRpcErrorObject
import io.github.vupoint.cokit.protocol.JsonRpcId
import io.github.vupoint.cokit.protocol.JsonRpcMessage
import io.github.vupoint.cokit.protocol.JsonRpcNotification
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString

/**
 * Correlates requests and routes incoming notifications and server requests.
 *
 * Collection starts in [scope] at construction. Incoming collection failures fail currently
 * pending requests; normal flow completion does not complete them. Requests have no built-in
 * timeout, retry, or concurrency limit, so callers should bound their lifetime and volume.
 * This low-level session does not perform the initialization handshake or answer server requests.
 *
 * Message sizes are checked by re-encoding envelopes as UTF-8 before sending or routing.
 * This limits routed payload size, not the transport's raw frame or parsing allocations.
 *
 * @param transport Transport owned by this session and closed by [close].
 * @param scope Caller-owned scope for incoming collection; closing the session cancels
 * only its collector job, not the scope.
 * @param maxMessageBytes Positive maximum encoded envelope size in bytes, inclusive;
 * defaults to [DEFAULT_MAX_MESSAGE_BYTES] (16 MiB).
 */
class JsonRpcSession(
    private val transport: JsonRpcTransport,
    private val scope: CoroutineScope,
    private val maxMessageBytes: Int = DEFAULT_MAX_MESSAGE_BYTES,
) : AutoCloseable {
    init {
        require(maxMessageBytes > 0) { "maxMessageBytes must be greater than zero" }
    }

    private var nextRequestId = 1L
    private var closed = false
    private val mutex = Mutex()
    private val pendingRequests = mutableMapOf<JsonRpcId, CompletableDeferred<JsonElementResult>>()
    private val mutableNotifications = MutableSharedFlow<JsonRpcNotification>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableServerRequests = MutableSharedFlow<JsonRpcRequest>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val collectorJob: Job = scope.launch {
        try {
            transport.incoming.collect { routeIncoming(it) }
        } catch (error: Throwable) {
            cancelPending(error)
        }
    }

    /**
     * Hot notification stream with no replay and a 64-message buffer that drops the oldest
     * buffered message when subscribers fall behind. Messages without subscribers are lost.
     * The flow does not complete when the session closes; collectors need their own lifetime.
     */
    val notifications: SharedFlow<JsonRpcNotification> = mutableNotifications

    /**
     * Hot server-request stream with no replay and a 64-message buffer that drops the oldest
     * buffered request when subscribers fall behind. Requests without subscribers are lost.
     * No response or approval is sent automatically, and closing the session does not complete
     * the flow. Applications must collect and respond with [sendResponse].
     */
    val serverRequests: SharedFlow<JsonRpcRequest> = mutableServerRequests

    /**
     * Sends a notification with no parameters or response tracking.
     *
     * @throws CancellationException If the session is closed.
     * @throws JsonRpcMessageSizeException If the encoded envelope exceeds the configured limit.
     */
    suspend fun notify(method: String) {
        requireOpen()
        val message = JsonRpcNotification(method = method)
        requireWithinMessageLimit(message)
        transport.send(message)
    }

    /**
     * Sends a request with an incrementing numeric identifier, without awaiting or retaining
     * its response. Incoming responses without a pending [request] correlation are ignored.
     *
     * @param method App-server method name.
     * @param params Optional raw parameters; absent by default.
     * @return Identifier assigned to the sent request.
     * @throws CancellationException If the session is closed.
     * @throws JsonRpcMessageSizeException If the encoded envelope exceeds the configured limit.
     */
    suspend fun sendRequest(method: String, params: JsonElementResult = null): JsonRpcId {
        val id = nextId()
        val message = JsonRpcRequest(id = id, method = method, params = params)
        requireWithinMessageLimit(message)
        transport.send(message)
        return id
    }

    /**
     * Sends a request and suspends until a matching response arrives.
     *
     * Cancellation removes the local pending correlation without cancelling remote work.
     * There is no built-in timeout or retry; use a caller-controlled coroutine timeout if needed.
     *
     * @param method App-server method name.
     * @param params Optional raw parameters; absent by default.
     * @return Raw response result, which may be `null`.
     * @throws JsonRpcRemoteException If the matching response contains an error.
     * @throws JsonRpcMessageSizeException If an outgoing envelope is oversized, or incoming
     * collection fails while this request is pending because an envelope is oversized.
     * @throws CancellationException If the caller is cancelled or the session closes.
     */
    suspend fun request(method: String, params: JsonElementResult = null): JsonElementResult =
        request(method, params) {}

    /**
     * Sends and awaits a request, reporting its identifier after [JsonRpcTransport.send] returns.
     *
     * The callback runs in the requesting coroutine before awaiting the response. A callback
     * failure or caller cancellation removes the pending correlation; remote work may continue.
     * Remote errors, transport failures, and size-limit failures propagate without retry.
     *
     * @param method App-server method name.
     * @param params Optional raw parameters.
     * @param onRequestId Callback receiving the assigned identifier after the send succeeds.
     * @return Raw response result, which may be `null`.
     */
    suspend fun request(
        method: String,
        params: JsonElementResult,
        onRequestId: (JsonRpcId) -> Unit,
    ): JsonElementResult {
        val id = nextId()
        val message = JsonRpcRequest(id = id, method = method, params = params)
        requireWithinMessageLimit(message)
        val deferred = CompletableDeferred<JsonElementResult>()
        mutex.withLock {
            if (closed) throw closedCancellationException()
            pendingRequests[id] = deferred
        }
        try {
            transport.send(message)
            onRequestId(id)
            return deferred.await()
        } catch (error: Throwable) {
            withContext(NonCancellable) { cancelPending(id, error) }
            throw error
        }
    }

    /**
     * Sends an application-created response, typically for a request from [serverRequests].
     * The caller is responsible for the matching identifier and the approval decision.
     *
     * @throws CancellationException If the session is closed.
     * @throws JsonRpcMessageSizeException If the encoded envelope exceeds the configured limit.
     */
    suspend fun sendResponse(response: JsonRpcResponse) {
        requireOpen()
        requireWithinMessageLimit(response)
        transport.send(response)
    }

    /**
     * Routes a decoded envelope directly for tests, applying the normal size check and correlation.
     * This bypasses the transport and does not check whether the session is closed.
     */
    suspend fun publishForTests(message: JsonRpcMessage) {
        routeIncoming(message)
    }

    private suspend fun nextId(): JsonRpcId = mutex.withLock {
        if (closed) throw closedCancellationException()
        JsonRpcId.Number(nextRequestId++)
    }

    private suspend fun requireOpen() {
        mutex.withLock {
            if (closed) throw closedCancellationException()
        }
    }

    private suspend fun routeIncoming(message: JsonRpcMessage) {
        requireWithinMessageLimit(message)
        when (message) {
            is JsonRpcResponse -> completeResponse(message)
            is JsonRpcNotification -> mutableNotifications.emit(message)
            is JsonRpcRequest -> mutableServerRequests.emit(message)
        }
    }

    private suspend fun completeResponse(response: JsonRpcResponse) {
        val deferred = mutex.withLock {
            pendingRequests.remove(response.id)
        } ?: return

        val error = response.error
        if (error != null) {
            deferred.completeExceptionally(JsonRpcRemoteException(error))
        } else {
            deferred.complete(response.result)
        }
    }

    private suspend fun cancelPending(id: JsonRpcId, error: Throwable) {
        val deferred = mutex.withLock {
            pendingRequests.remove(id)
        } ?: return
        deferred.completeExceptionally(error)
    }

    private suspend fun cancelPending(error: Throwable) {
        val requests = mutex.withLock {
            pendingRequests.values.toList().also { pendingRequests.clear() }
        }
        requests.forEach { it.completeExceptionally(error) }
    }

    /**
     * Cancels incoming collection, closes the owned transport, and cancels pending requests.
     * Repeated calls do nothing. The supplied scope and the exposed shared flows remain open.
     */
    override fun close() {
        if (closed) return
        closed = true
        collectorJob.cancel()
        transport.close()
        val cancellation = closedCancellationException()
        pendingRequests.values.forEach { it.completeExceptionally(cancellation) }
        pendingRequests.clear()
    }

    private fun requireWithinMessageLimit(message: JsonRpcMessage) {
        val actualMessageBytes = CodexProtocolJson.encodeToString(message).encodeToByteArray().size
        if (actualMessageBytes > maxMessageBytes) {
            throw JsonRpcMessageSizeException(
                actualMessageBytes = actualMessageBytes,
                maxMessageBytes = maxMessageBytes,
            )
        }
    }

    /** Default session size limit. */
    companion object {
        /** Maximum encoded envelope size in bytes used by default: 16 MiB. */
        const val DEFAULT_MAX_MESSAGE_BYTES: Int = 16 * 1024 * 1024
    }
}

private fun closedCancellationException(): CancellationException =
    CancellationException("JSON-RPC session closed")

/** Raw JSON request parameters or response results, including an absent or null value. */
typealias JsonElementResult = kotlinx.serialization.json.JsonElement?

/**
 * Failure reported in a matching JSON-RPC response, with the original server error retained.
 *
 * @property error Server-provided code, message, and optional raw details.
 */
class JsonRpcRemoteException(
    val error: JsonRpcErrorObject,
) : RuntimeException(error.message) {
    /** Whether the code signals server overload; this hint does not trigger an automatic retry. */
    val isRetryableOverload: Boolean
        get() = error.code == SERVER_OVERLOADED_CODE

    /** Recognized app-server error codes. */
    companion object {
        /** App-server overload error code used by [isRetryableOverload]. */
        const val SERVER_OVERLOADED_CODE: Int = -32001
    }
}

/**
 * An envelope exceeded a session's limit when re-encoded as UTF-8 JSON.
 *
 * @property actualMessageBytes Encoded envelope size in bytes, excluding transport framing.
 * @property maxMessageBytes Configured inclusive maximum size in bytes.
 */
class JsonRpcMessageSizeException(
    val actualMessageBytes: Int,
    val maxMessageBytes: Int,
) : RuntimeException(
    "JSON-RPC message is $actualMessageBytes bytes, exceeding the configured $maxMessageBytes byte limit",
)
