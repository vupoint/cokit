@file:OptIn(ExperimentalCodexApi::class)

package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.approvals.ApprovalDecision
import io.github.vupoint.cokit.client.approvals.CommandApprovalHandler
import io.github.vupoint.cokit.client.approvals.CommandApprovalKind
import io.github.vupoint.cokit.client.approvals.FileChangeApprovalHandler
import io.github.vupoint.cokit.client.approvals.PermissionApprovalHandler
import io.github.vupoint.cokit.client.attestation.AttestationGenerateHandler
import io.github.vupoint.cokit.client.mcp.McpElicitationHandler
import io.github.vupoint.cokit.client.server.UserInputRequestHandler
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.client.tools.DynamicToolCallHandler
import io.github.vupoint.cokit.client.tools.DynamicToolCallResponse
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class ServerRequestDispatcher(
    private val experimentalApi: Boolean,
    private val requiredHumanApproval: Boolean,
) {
    private var commandApprovalHandler: CommandApprovalHandler? = null
    private var humanCommandApprovalHandler: CommandApprovalHandler? = null
    private var fileChangeApprovalHandler: FileChangeApprovalHandler? = null
    private var permissionApprovalHandler: PermissionApprovalHandler? = null
    private var userInputRequestHandler: UserInputRequestHandler? = null
    private var mcpElicitationHandler: McpElicitationHandler? = null
    private var dynamicToolCallHandler: DynamicToolCallHandler? = null
    private var attestationGenerateHandler: AttestationGenerateHandler? = null

    fun registerCommandApprovalHandler(handler: CommandApprovalHandler) {
        commandApprovalHandler = handler
    }

    fun registerHumanCommandApprovalHandler(handler: CommandApprovalHandler) {
        require(experimentalApi && requiredHumanApproval) {
            "Human approvals require experimentalApi and requiredHumanApproval initialization capabilities"
        }
        humanCommandApprovalHandler = handler
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

    fun registerDynamicToolCallHandler(handler: DynamicToolCallHandler) {
        require(experimentalApi) { "Dynamic tools require InitializeCapabilities(experimentalApi = true)" }
        dynamicToolCallHandler = handler
    }

    suspend fun resolve(request: JsonRpcRequest): JsonRpcResponse {
        if (request.method == "item/tool/call") {
            val call = try {
                request.decodeDynamicToolCallRequest()
            } catch (_: Exception) {
                return JsonRpcResponse(request.id, error = invalidServerRequestParamsError(request.method))
            }
            return try {
                val result = if (experimentalApi) dynamicToolCallHandler?.call(call) else null
                JsonRpcResponse(request.id, result = (result ?: DynamicToolCallResponse(emptyList(), false)).toProtocolJson())
            } catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                JsonRpcResponse(request.id, error = serverRequestHandlerError())
            }
        }

        if (request.method == "item/commandExecution/requestApproval" &&
            (commandApprovalHandler != null || humanCommandApprovalHandler != null)
        ) {
            val commandRequest = try {
                request.decodeCommandApprovalRequest()
            } catch (error: Throwable) {
                return JsonRpcResponse(
                    id = request.id,
                    error = invalidServerRequestParamsError(request.method),
                )
            }
            return try {
                val isHuman = commandRequest.kind == CommandApprovalKind.HumanCommand
                val handler = if (isHuman) humanCommandApprovalHandler else commandApprovalHandler
                val decision = handler?.decide(commandRequest) ?: ApprovalDecision.Decline
                val oneTimeDecision = if (isHuman && decision == ApprovalDecision.AcceptForSession) {
                    ApprovalDecision.Decline
                } else decision
                JsonRpcResponse(
                    id = request.id,
                    result = oneTimeDecision.toProtocolJson(),
                )
            } catch (error: Throwable) {
                currentCoroutineContext().ensureActive()
                JsonRpcResponse(
                    id = request.id,
                    error = serverRequestHandlerError(),
                )
            }
        }

        val fileChangeHandler = fileChangeApprovalHandler
        if (request.method == "item/fileChange/requestApproval" && fileChangeHandler != null) {
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
                    result = fileChangeHandler.decide(fileChangeRequest).toProtocolJson(),
                )
            } catch (error: Throwable) {
                JsonRpcResponse(
                    id = request.id,
                    error = serverRequestHandlerError(),
                )
            }
        }

        val permissionHandler = permissionApprovalHandler
        if (request.method == "item/permissions/requestApproval" && permissionHandler != null) {
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
                    result = permissionHandler.decide(permissionRequest).toProtocolJson(),
                )
            } catch (error: Throwable) {
                JsonRpcResponse(
                    id = request.id,
                    error = serverRequestHandlerError(),
                )
            }
        }

        val userInputHandler = userInputRequestHandler
        if (request.method == "item/tool/requestUserInput" && userInputHandler != null) {
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
                    result = userInputHandler.respond(userInputRequest).toProtocolJson(),
                )
            } catch (error: Throwable) {
                JsonRpcResponse(
                    id = request.id,
                    error = serverRequestHandlerError(),
                )
            }
        }

        val mcpHandler = mcpElicitationHandler
        if (request.method == "mcpServer/elicitation/request" && mcpHandler != null) {
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
                    result = mcpHandler.respond(mcpRequest).toProtocolJson(),
                )
            } catch (error: Throwable) {
                JsonRpcResponse(
                    id = request.id,
                    error = serverRequestHandlerError(),
                )
            }
        }

        val attestationHandler = attestationGenerateHandler
        if (request.method == "attestation/generate" && attestationHandler != null) {
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
                    result = attestationHandler.generate(attestationRequest).toProtocolJson(),
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
            JsonRpcResponse(id = request.id, result = defaultResult)
        } else {
            JsonRpcResponse(id = request.id, error = noHandlerError(request.method))
        }
    }
}
