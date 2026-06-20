package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.approvals.CommandApprovalHandler
import io.github.vupoint.cokit.client.approvals.FileChangeApprovalHandler
import io.github.vupoint.cokit.client.approvals.PermissionApprovalHandler
import io.github.vupoint.cokit.client.attestation.AttestationGenerateHandler
import io.github.vupoint.cokit.client.mcp.McpElicitationHandler
import io.github.vupoint.cokit.client.server.UserInputRequestHandler
import io.github.vupoint.cokit.protocol.CodexProtocolJson
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.rpc.JsonRpcSession
import io.github.vupoint.cokit.rpc.JsonRpcTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.encodeToJsonElement

data class CodexRpcConnection(
    val transport: JsonRpcTransport,
    val clientInfo: ClientInfo,
    val scope: CoroutineScope,
    val capabilities: InitializeCapabilities? = null,
)

class CodexRpcClient private constructor(
    private val rpc: JsonRpcSession,
    scope: CoroutineScope,
) : AutoCloseable {
    private val mutableServerRequests = MutableSharedFlow<CodexServerRequest>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableNotifications = MutableSharedFlow<CodexNotification>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private var commandApprovalHandler: CommandApprovalHandler? = null
    private var fileChangeApprovalHandler: FileChangeApprovalHandler? = null
    private var permissionApprovalHandler: PermissionApprovalHandler? = null
    private var userInputRequestHandler: UserInputRequestHandler? = null
    private var mcpElicitationHandler: McpElicitationHandler? = null
    private var attestationGenerateHandler: AttestationGenerateHandler? = null
    private val serverRequestJob: Job = scope.launch {
        rpc.serverRequests.collect { request ->
            mutableServerRequests.tryEmit(request.toCodexServerRequest())
            rpc.sendResponse(resolveServerRequest(request))
        }
    }
    private val notificationJob: Job = scope.launch {
        rpc.notifications.collect { notification ->
            mutableNotifications.tryEmit(notification.toCodexNotification())
        }
    }

    val notifications: SharedFlow<CodexNotification> = mutableNotifications
    val serverRequests: SharedFlow<CodexServerRequest> = mutableServerRequests
    val isInitialized: Boolean = true

    suspend fun <P : Any, R : Any> request(
        method: CodexRpcMethod<P, R>,
        params: P,
    ): R = rpc.request(method, params)

    fun registerCommandApprovalHandler(handler: CommandApprovalHandler) {
        commandApprovalHandler = handler
    }

    fun registerFileChangeApprovalHandler(handler: FileChangeApprovalHandler) {
        fileChangeApprovalHandler = handler
    }

    fun registerPermissionApprovalHandler(handler: PermissionApprovalHandler) {
        permissionApprovalHandler = handler
    }

    fun registerUserInputRequestHandler(handler: UserInputRequestHandler) {
        userInputRequestHandler = handler
    }

    fun registerMcpElicitationHandler(handler: McpElicitationHandler) {
        mcpElicitationHandler = handler
    }

    fun registerAttestationGenerateHandler(handler: AttestationGenerateHandler) {
        attestationGenerateHandler = handler
    }

    override fun close() {
        notificationJob.cancel()
        serverRequestJob.cancel()
        rpc.close()
    }

    private suspend fun resolveServerRequest(request: JsonRpcRequest): JsonRpcResponse {
        val handler = commandApprovalHandler
        if (request.method == COMMAND_APPROVAL_METHOD && handler != null) {
            val commandRequest = try {
                request.decodeCommandApprovalRequest()
            } catch (error: Throwable) {
                return JsonRpcResponse(
                    id = request.id,
                    error = invalidServerRequestParamsError(request.method),
                )
            }
            return try {
                JsonRpcResponse(
                    id = request.id,
                    result = handler.decide(commandRequest).toProtocolPayload().toJsonElement(),
                )
            } catch (error: Throwable) {
                JsonRpcResponse(
                    id = request.id,
                    error = serverRequestHandlerError(),
                )
            }
        }

        val fileChangeHandler = fileChangeApprovalHandler
        if (request.method == FILE_CHANGE_APPROVAL_METHOD && fileChangeHandler != null) {
            val fileChangeRequest = try {
                request.decodeFileChangeApprovalRequest()
            } catch (error: Throwable) {
                return JsonRpcResponse(
                    id = request.id,
                    error = invalidServerRequestParamsError(request.method),
                )
            }
            return try {
                JsonRpcResponse(
                    id = request.id,
                    result = fileChangeHandler.decide(fileChangeRequest).toProtocolPayload().toJsonElement(),
                )
            } catch (error: Throwable) {
                JsonRpcResponse(
                    id = request.id,
                    error = serverRequestHandlerError(),
                )
            }
        }

        val permissionHandler = permissionApprovalHandler
        if (request.method == PERMISSION_APPROVAL_METHOD && permissionHandler != null) {
            val permissionRequest = try {
                request.decodePermissionApprovalRequest()
            } catch (error: Throwable) {
                return JsonRpcResponse(
                    id = request.id,
                    error = invalidServerRequestParamsError(request.method),
                )
            }
            return try {
                JsonRpcResponse(
                    id = request.id,
                    result = permissionHandler.decide(permissionRequest).toProtocolPayload().toJsonElement(),
                )
            } catch (error: Throwable) {
                JsonRpcResponse(
                    id = request.id,
                    error = serverRequestHandlerError(),
                )
            }
        }

        val userInputHandler = userInputRequestHandler
        if (request.method == USER_INPUT_REQUEST_METHOD && userInputHandler != null) {
            val userInputRequest = try {
                request.decodeUserInputRequest()
            } catch (error: Throwable) {
                return JsonRpcResponse(
                    id = request.id,
                    error = invalidServerRequestParamsError(request.method),
                )
            }
            return try {
                JsonRpcResponse(
                    id = request.id,
                    result = userInputHandler.respond(userInputRequest).toProtocolPayload().toJsonElement(),
                )
            } catch (error: Throwable) {
                JsonRpcResponse(
                    id = request.id,
                    error = serverRequestHandlerError(),
                )
            }
        }

        val mcpHandler = mcpElicitationHandler
        if (request.method == MCP_ELICITATION_REQUEST_METHOD && mcpHandler != null) {
            val mcpRequest = try {
                request.decodeMcpElicitationRequest()
            } catch (error: Throwable) {
                return JsonRpcResponse(
                    id = request.id,
                    error = invalidServerRequestParamsError(request.method),
                )
            }
            return try {
                JsonRpcResponse(
                    id = request.id,
                    result = mcpHandler.respond(mcpRequest).toProtocolPayload().toJsonElement(),
                )
            } catch (error: Throwable) {
                JsonRpcResponse(
                    id = request.id,
                    error = serverRequestHandlerError(),
                )
            }
        }

        val attestationHandler = attestationGenerateHandler
        if (request.method == ATTESTATION_GENERATE_METHOD && attestationHandler != null) {
            val attestationRequest = try {
                request.decodeAttestationGenerateRequest()
            } catch (error: Throwable) {
                return JsonRpcResponse(
                    id = request.id,
                    error = invalidServerRequestParamsError(request.method),
                )
            }
            return try {
                JsonRpcResponse(
                    id = request.id,
                    result = attestationHandler.generate(attestationRequest).toProtocolPayload().toJsonElement(),
                )
            } catch (error: Throwable) {
                JsonRpcResponse(
                    id = request.id,
                    error = serverRequestHandlerError(),
                )
            }
        }

        val defaultResult = defaultServerRequestResult(request.method)
        return if (defaultResult != null) {
            JsonRpcResponse(id = request.id, result = defaultResult.toJsonElement())
        } else {
            JsonRpcResponse(id = request.id, error = noHandlerError(request.method))
        }
    }

    companion object {
        suspend fun connect(connection: CodexRpcConnection): CodexRpcClient {
            val session = JsonRpcSession(connection.transport, connection.scope)
            return try {
                val params = InitializeParams(
                    clientInfo = connection.clientInfo,
                    capabilities = connection.capabilities,
                )
                session.request(
                    method = "initialize",
                    params = CodexProtocolJson.encodeToJsonElement(InitializeParams.serializer(), params),
                )
                session.notify("initialized")
                CodexRpcClient(session, connection.scope)
            } catch (error: Throwable) {
                session.close()
                throw error
            }
        }
    }
}
