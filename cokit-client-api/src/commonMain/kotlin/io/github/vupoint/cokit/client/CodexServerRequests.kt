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

internal const val COMMAND_APPROVAL_METHOD = "item/commandExecution/requestApproval"
internal const val FILE_CHANGE_APPROVAL_METHOD = "item/fileChange/requestApproval"
internal const val PERMISSION_APPROVAL_METHOD = "item/permissions/requestApproval"
internal const val USER_INPUT_REQUEST_METHOD = "item/tool/requestUserInput"
internal const val MCP_ELICITATION_REQUEST_METHOD = "mcpServer/elicitation/request"
internal const val ATTESTATION_GENERATE_METHOD = "attestation/generate"

sealed interface CodexServerRequest {
    val method: String

    data class CommandApproval(
        val request: CommandApprovalRequest,
    ) : CodexServerRequest {
        override val method: String = COMMAND_APPROVAL_METHOD
    }

    data class FileChangeApproval(
        val request: FileChangeApprovalRequest,
    ) : CodexServerRequest {
        override val method: String = FILE_CHANGE_APPROVAL_METHOD
    }

    data class PermissionApproval(
        val request: PermissionApprovalRequest,
    ) : CodexServerRequest {
        override val method: String = PERMISSION_APPROVAL_METHOD
    }

    data class UserInput(
        val request: UserInputRequest,
    ) : CodexServerRequest {
        override val method: String = USER_INPUT_REQUEST_METHOD
    }

    data class McpElicitation(
        val request: McpElicitationRequest,
    ) : CodexServerRequest {
        override val method: String = MCP_ELICITATION_REQUEST_METHOD
    }

    data class AttestationGenerate(
        val request: AttestationGenerateRequest,
    ) : CodexServerRequest {
        override val method: String = ATTESTATION_GENERATE_METHOD
    }

    data class Unsupported(
        override val method: String,
    ) : CodexServerRequest
}
