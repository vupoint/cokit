package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.approvals.CommandApprovalHandler
import io.github.vupoint.cokit.client.approvals.FileChangeApprovalHandler
import io.github.vupoint.cokit.client.approvals.PermissionApprovalHandler
import io.github.vupoint.cokit.client.attestation.AttestationGenerateHandler
import io.github.vupoint.cokit.client.mcp.McpElicitationHandler
import io.github.vupoint.cokit.client.server.UserInputRequestHandler
import io.github.vupoint.cokit.client.tools.DynamicToolCallHandler
import io.github.vupoint.cokit.rpc.JsonRpcTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow

/**
 * Dependencies and handshake settings for creating a [CodexClient].
 *
 * @property transport Transport owned by the client; it is closed on client shutdown or a failed handshake.
 * @property clientInfo Application identity sent to the app-server during initialization.
 * @property scope Caller-owned scope for incoming messages and server-request handling. Keep it active
 * for the connection's lifetime; closing the client does not cancel the scope itself.
 * @property capabilities Optional initialization flags. Experimental APIs require an explicit opt-in here.
 */
data class CodexClientConnection(
    val transport: JsonRpcTransport,
    val clientInfo: ClientInfo,
    val scope: CoroutineScope,
    val capabilities: InitializeCapabilities? = null,
)

/**
 * Initialized app-server connection with typed requests, thread/turn helpers, and incoming events.
 *
 * Approval-like requests are declined, cancelled, or reported unsupported until an application
 * registers the corresponding handler. Collecting [serverRequests] does not authorize a request.
 * Close the client when finished to release its transport and cancel pending requests.
 */
interface CodexClient : AutoCloseable {
    /** Live notifications with no replay; slow collectors can lose older buffered events. */
    val notifications: SharedFlow<CodexNotification>
    /** Live observations of server requests; register handlers below to provide their responses. */
    val serverRequests: SharedFlow<CodexServerRequest>
    /** Convenience operations that return thread models directly. */
    val threads: ThreadsApi
    /** Convenience operations for starting, steering, and interrupting turns. */
    val turns: TurnsApi
    /** Whether the initialization handshake has completed; this is not a connection-health check. */
    val isInitialized: Boolean

    /**
     * Sends a typed request and waits for its correlated result.
     *
     * Use [CodexRpc] descriptors for operations outside the convenience APIs. Server errors and
     * decoding failures propagate to the caller. Apply a coroutine timeout when a bounded wait is
     * needed; cancelling the wait does not cancel server-side work.
     *
     * @throws IllegalArgumentException if the descriptor requires experimental initialization
     * capabilities that were not enabled.
     */
    suspend fun <P : Any, R : Any> request(
        method: CodexRpcMethod<P, R>,
        params: P,
    ): R

    /**
     * Sends a typed request, reports its ID through [onRequestId] after sending, and awaits the result.
     *
     * The callback runs in the requesting coroutine before it waits for the response. Record the ID
     * for operations that support request-ID cancellation, such as [CodexRpc.UserVerification.Cancel].
     * Cancelling this coroutine alone only stops the local wait.
     */
    suspend fun <P : Any, R : Any> request(
        method: CodexRpcMethod<P, R>,
        params: P,
        onRequestId: (CodexRequestId) -> Unit,
    ): R

    /** Replaces the command-approval handler; unhandled command approvals are declined. */
    fun registerCommandApprovalHandler(handler: CommandApprovalHandler)

    /** Replaces the file-change handler; unhandled file changes are declined. */
    fun registerFileChangeApprovalHandler(handler: FileChangeApprovalHandler)

    /** Replaces the permission handler; unhandled requests receive no permission grants. */
    fun registerPermissionApprovalHandler(handler: PermissionApprovalHandler)

    /** Replaces the user-input handler; unhandled requests receive a cancellation response. */
    fun registerUserInputRequestHandler(handler: UserInputRequestHandler)

    /** Replaces the MCP elicitation handler; unhandled elicitations are declined. */
    fun registerMcpElicitationHandler(handler: McpElicitationHandler)

    /** Replaces the attestation provider; unhandled attestations are reported unsupported. */
    fun registerAttestationGenerateHandler(handler: AttestationGenerateHandler)

    /**
     * Replaces the application's dynamic-tool executor. Unhandled calls return an unsuccessful result.
     *
     * Requires [InitializeCapabilities.experimentalApi] to be enabled as well as Kotlin opt-in to
     * [ExperimentalCodexApi]. The application is responsible for authorizing tool side effects.
     */
    @ExperimentalCodexApi
    fun registerDynamicToolCallHandler(handler: DynamicToolCallHandler)
}
