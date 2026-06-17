package io.github.cokit.client

import io.github.cokit.client.approvals.ApprovalDecision
import io.github.cokit.client.approvals.CommandApprovalRequest
import io.github.cokit.protocol.CodexProtocolJson
import io.github.cokit.protocol.JsonRpcErrorObject
import io.github.cokit.protocol.JsonRpcRequest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put

internal const val COMMAND_APPROVAL_METHOD = "item/commandExecution/requestApproval"

sealed interface CodexServerRequest {
    val method: String

    data class CommandApproval(
        val request: CommandApprovalRequest,
    ) : CodexServerRequest {
        override val method: String = COMMAND_APPROVAL_METHOD
    }

    data class Unsupported(
        override val method: String,
    ) : CodexServerRequest
}

data class CodexRawServerRequest(
    val method: String,
    val params: CodexJsonPayload? = null,
)

internal fun JsonRpcRequest.toCodexServerRequest(): CodexServerRequest {
    return when (method) {
        COMMAND_APPROVAL_METHOD -> runCatching {
            CodexServerRequest.CommandApproval(decodeCommandApprovalRequest())
        }.getOrElse {
            CodexServerRequest.Unsupported(method)
        }
        else -> CodexServerRequest.Unsupported(method)
    }
}

internal fun JsonRpcRequest.decodeCommandApprovalRequest(): CommandApprovalRequest {
    val paramsElement = requireNotNull(params) {
        "Expected command approval request params"
    }
    return CodexProtocolJson.decodeFromJsonElement(CommandApprovalRequest.serializer(), paramsElement)
}

internal fun ApprovalDecision.toProtocolPayload(): CodexJsonPayload {
    val value = when (this) {
        ApprovalDecision.Accept -> "accept"
        ApprovalDecision.AcceptForSession -> "accept_for_session"
        ApprovalDecision.Decline -> "decline"
        ApprovalDecision.Cancel -> "cancel"
    }
    return buildJsonObject {
        put("decision", value)
    }.toCodexPayload()
}

internal fun defaultServerRequestResult(method: String): CodexJsonPayload? {
    return when (method) {
        COMMAND_APPROVAL_METHOD,
        "item/fileChange/requestApproval",
        "item/permissions/requestApproval",
        "item/tool/call",
        "mcpServer/elicitation/request",
        -> buildJsonObject { put("decision", "decline") }.toCodexPayload()

        "item/tool/requestUserInput" -> buildJsonObject { put("decision", "cancel") }.toCodexPayload()
        "attestation/generate" -> buildJsonObject { put("status", "unsupported") }.toCodexPayload()
        else -> null
    }
}

internal fun noHandlerError(method: String): JsonRpcErrorObject {
    return JsonRpcErrorObject(
        code = -32601,
        message = "No handler registered for $method",
    )
}

internal fun invalidServerRequestParamsError(method: String): JsonRpcErrorObject {
    return JsonRpcErrorObject(
        code = -32602,
        message = "Invalid params for $method",
    )
}

internal fun serverRequestHandlerError(): JsonRpcErrorObject {
    return JsonRpcErrorObject(
        code = -32000,
        message = "Server request handler failed",
    )
}
