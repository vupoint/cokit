package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.approvals.CommandApprovalRequest
import io.github.vupoint.cokit.client.approvals.FileChangeApprovalRequest
import io.github.vupoint.cokit.client.approvals.PermissionApprovalRequest
import io.github.vupoint.cokit.client.approvals.PermissionApprovalResponse
import io.github.vupoint.cokit.client.attestation.AttestationGenerateRequest
import io.github.vupoint.cokit.client.attestation.AttestationGenerateResponse
import io.github.vupoint.cokit.client.mcp.McpElicitationRequest
import io.github.vupoint.cokit.client.mcp.McpElicitationResponse
import io.github.vupoint.cokit.client.server.UserInputRequest
import io.github.vupoint.cokit.client.server.UserInputResponse
import io.github.vupoint.cokit.client.tools.DynamicToolCallRequest

internal const val COMMAND_APPROVAL_METHOD = "item/commandExecution/requestApproval"
internal const val FILE_CHANGE_APPROVAL_METHOD = "item/fileChange/requestApproval"
internal const val PERMISSION_APPROVAL_METHOD = "item/permissions/requestApproval"
internal const val USER_INPUT_REQUEST_METHOD = "item/tool/requestUserInput"
internal const val MCP_ELICITATION_REQUEST_METHOD = "mcpServer/elicitation/request"
internal const val ATTESTATION_GENERATE_METHOD = "attestation/generate"

/**
 * Typed view of a server-initiated request requiring a client response.
 *
 * Without an explicit application handler, approvals are declined, user input is cancelled,
 * tool calls fail, and attestation reports unsupported. Observe [CodexClient.serverRequests]
 * for request events.
 */
sealed interface CodexServerRequest {
    val method: String

    /** Server request asking the application to decide whether command execution may proceed. */
    data class CommandApproval(
        val request: CommandApprovalRequest,
    ) : CodexServerRequest {
        override val method: String = COMMAND_APPROVAL_METHOD
    }

    /** Server request asking the application to decide whether proposed file changes may proceed. */
    data class FileChangeApproval(
        val request: FileChangeApprovalRequest,
    ) : CodexServerRequest {
        override val method: String = FILE_CHANGE_APPROVAL_METHOD
    }

    /** Server request asking the application to decide whether additional permissions may be granted. */
    data class PermissionApproval(
        val request: PermissionApprovalRequest,
    ) : CodexServerRequest {
        override val method: String = PERMISSION_APPROVAL_METHOD
    }

    /** Server request for answers to structured questions; observation alone does not answer it. */
    data class UserInput(
        val request: UserInputRequest,
    ) : CodexServerRequest {
        override val method: String = USER_INPUT_REQUEST_METHOD
    }

    /** Server request for MCP elicitation input, dispatched to an explicitly registered handler. */
    data class McpElicitation(
        val request: McpElicitationRequest,
    ) : CodexServerRequest {
        override val method: String = MCP_ELICITATION_REQUEST_METHOD
    }

    /** Server request for attestation output, requiring an explicitly registered handler. */
    data class AttestationGenerate(
        val request: AttestationGenerateRequest,
    ) : CodexServerRequest {
        override val method: String = ATTESTATION_GENERATE_METHOD
    }

    /** Experimental request to execute an application-provided dynamic tool. */
    @ExperimentalCodexApi
    data class DynamicToolCall(
        val request: DynamicToolCallRequest,
    ) : CodexServerRequest {
        override val method: String = "item/tool/call"
    }

    /** Unrecognized server-request method. This view retains the method name, not its raw parameters. */
    data class Unsupported(
        override val method: String,
    ) : CodexServerRequest
}
