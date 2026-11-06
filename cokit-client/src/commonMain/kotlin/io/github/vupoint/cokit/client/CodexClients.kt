@file:OptIn(ExperimentalCodexApi::class)

package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.approvals.CommandApprovalHandler
import io.github.vupoint.cokit.client.approvals.FileChangeApprovalHandler
import io.github.vupoint.cokit.client.approvals.PermissionApprovalHandler
import io.github.vupoint.cokit.client.attestation.AttestationGenerateHandler
import io.github.vupoint.cokit.client.mcp.McpElicitationHandler
import io.github.vupoint.cokit.client.server.UserInputRequestHandler
import io.github.vupoint.cokit.protocol.CodexProtocolJson
import io.github.vupoint.cokit.rpc.JsonRpcSession
import io.github.vupoint.cokit.client.tools.DynamicToolCallHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.encodeToJsonElement

/** Factory for the default [CodexClient] implementation backed by an app-server transport. */
object CodexClients {
    /**
     * Sends `initialize`, awaits its response, then sends `initialized` before returning the client.
     *
     * The returned client owns [CodexClientConnection.transport] and uses the caller's scope for
     * incoming messages. Keep that scope active and close the client when finished. A failed or
     * cancelled handshake closes the transport and propagates the error. No timeout is imposed;
     * wrap the call in a coroutine timeout when needed.
     *
     * Enable [InitializeCapabilities.experimentalApi] in [connection] before using experimental
     * descriptors or dynamic tools. Approval-like requests remain deny-by-default until handlers
     * are registered on the returned client.
     */
    suspend fun connect(connection: CodexClientConnection): CodexClient {
        val session = JsonRpcSession(connection.transport, connection.scope)
        return try {
            require(connection.capabilities?.requiredHumanApproval == null || connection.capabilities?.experimentalApi == true) {
                "Human approvals require InitializeCapabilities(experimentalApi = true)"
            }
            val params = InitializeParams(
                clientInfo = connection.clientInfo,
                capabilities = connection.capabilities,
            )
            session.request(
                method = "initialize",
                params = CodexProtocolJson.encodeToJsonElement(InitializeParams.serializer(), params),
            )
            session.notify("initialized")
            DefaultCodexClient(
                rpc = session,
                scope = connection.scope,
                experimentalApi = connection.capabilities?.experimentalApi == true,
                requiredHumanApproval = connection.capabilities?.requiredHumanApproval?.enabled == true,
            )
        } catch (error: Throwable) {
            session.close()
            throw error
        }
    }
}

internal class DefaultCodexClient(
    private val rpc: JsonRpcSession,
    scope: CoroutineScope,
    experimentalApi: Boolean = false,
    requiredHumanApproval: Boolean = false,
) : CodexClient {
    private val mutableServerRequests = MutableSharedFlow<CodexServerRequest>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableNotifications = MutableSharedFlow<CodexNotification>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val serverRequestDispatcher = ServerRequestDispatcher(experimentalApi, requiredHumanApproval)
    private val requests = ClientRequestExecutor(rpc, experimentalApi)
    private val serverRequestJob: Job = scope.launch {
        rpc.serverRequests.collect { request ->
            mutableServerRequests.tryEmit(request.toCodexServerRequest())
            rpc.sendResponse(serverRequestDispatcher.resolve(request))
        }
    }
    private val notificationJob: Job = scope.launch {
        rpc.notifications.collect { notification ->
            mutableNotifications.tryEmit(notification.toCodexNotification())
        }
    }

    override val notifications: SharedFlow<CodexNotification> = mutableNotifications
    override val serverRequests: SharedFlow<CodexServerRequest> = mutableServerRequests
    override val threads: ThreadsApi = DefaultThreadsApi(requests)
    override val turns: TurnsApi = DefaultTurnsApi(requests)
    override val isInitialized: Boolean = true

    override suspend fun <P : Any, R : Any> request(
        method: CodexRpcMethod<P, R>,
        params: P,
    ): R = request(method, params) {}

    override suspend fun <P : Any, R : Any> request(
        method: CodexRpcMethod<P, R>,
        params: P,
        onRequestId: (CodexRequestId) -> Unit,
    ): R {
        return requests.request(method, params, onRequestId)
    }

    override fun registerCommandApprovalHandler(handler: CommandApprovalHandler) {
        serverRequestDispatcher.registerCommandApprovalHandler(handler)
    }

    override fun registerHumanCommandApprovalHandler(handler: CommandApprovalHandler) {
        serverRequestDispatcher.registerHumanCommandApprovalHandler(handler)
    }

    override fun registerFileChangeApprovalHandler(handler: FileChangeApprovalHandler) {
        serverRequestDispatcher.registerFileChangeApprovalHandler(handler)
    }

    override fun registerPermissionApprovalHandler(handler: PermissionApprovalHandler) {
        serverRequestDispatcher.registerPermissionApprovalHandler(handler)
    }

    override fun registerUserInputRequestHandler(handler: UserInputRequestHandler) {
        serverRequestDispatcher.registerUserInputRequestHandler(handler)
    }

    override fun registerMcpElicitationHandler(handler: McpElicitationHandler) {
        serverRequestDispatcher.registerMcpElicitationHandler(handler)
    }

    override fun registerAttestationGenerateHandler(handler: AttestationGenerateHandler) {
        serverRequestDispatcher.registerAttestationGenerateHandler(handler)
    }

    override fun registerDynamicToolCallHandler(handler: DynamicToolCallHandler) {
        serverRequestDispatcher.registerDynamicToolCallHandler(handler)
    }

    override fun close() {
        notificationJob.cancel()
        serverRequestJob.cancel()
        rpc.close()
    }
}
