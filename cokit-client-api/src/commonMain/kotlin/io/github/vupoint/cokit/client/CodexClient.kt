package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.approvals.CommandApprovalHandler
import io.github.vupoint.cokit.client.approvals.FileChangeApprovalHandler
import io.github.vupoint.cokit.client.approvals.PermissionApprovalHandler
import io.github.vupoint.cokit.client.attestation.AttestationGenerateHandler
import io.github.vupoint.cokit.client.mcp.McpElicitationHandler
import io.github.vupoint.cokit.client.server.UserInputRequestHandler
import io.github.vupoint.cokit.rpc.JsonRpcTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow

data class CodexClientConnection(
    val transport: JsonRpcTransport,
    val clientInfo: ClientInfo,
    val scope: CoroutineScope,
    val capabilities: InitializeCapabilities? = null,
)

interface CodexClient : AutoCloseable {
    val notifications: SharedFlow<CodexNotification>
    val serverRequests: SharedFlow<CodexServerRequest>
    val threads: ThreadsApi
    val turns: TurnsApi
    val isInitialized: Boolean

    suspend fun <P : Any, R : Any> request(
        method: CodexRpcMethod<P, R>,
        params: P,
    ): R

    fun registerCommandApprovalHandler(handler: CommandApprovalHandler)

    fun registerFileChangeApprovalHandler(handler: FileChangeApprovalHandler)

    fun registerPermissionApprovalHandler(handler: PermissionApprovalHandler)

    fun registerUserInputRequestHandler(handler: UserInputRequestHandler)

    fun registerMcpElicitationHandler(handler: McpElicitationHandler)

    fun registerAttestationGenerateHandler(handler: AttestationGenerateHandler)
}
