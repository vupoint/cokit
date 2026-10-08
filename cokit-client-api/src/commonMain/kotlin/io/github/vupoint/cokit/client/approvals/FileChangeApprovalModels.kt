package io.github.vupoint.cokit.client.approvals

import io.github.vupoint.cokit.client.CodexHostPath
import io.github.vupoint.cokit.client.ItemId
import io.github.vupoint.cokit.client.ThreadId
import io.github.vupoint.cokit.client.TurnId
import kotlinx.serialization.Serializable

/**
 * Server request to approve a file-change item. CoKit declines it when no application handler is registered.
 *
 * @property startedAtMs Server-reported request start time in Unix epoch milliseconds.
 * @property grantRoot Optional host directory for which the server requests write access.
 */
@Serializable
data class FileChangeApprovalRequest(
    val threadId: ThreadId,
    val turnId: TurnId,
    val itemId: ItemId,
    val startedAtMs: Long,
    val reason: String? = null,
    val grantRoot: CodexHostPath? = null,
)

/** Host file change reported by app-server, with an optional textual diff for inspection. */
@Serializable
data class FileChangeSummary(
    val path: CodexHostPath,
    val kind: FileChangeKind,
    val diff: String? = null,
)

/** Server-reported file operation kind; unknown wire strings are retained. */
@Serializable
@JvmInline
value class FileChangeKind(val value: String)

/** Application policy callback for file mutations. CoKit sends [ApprovalDecision.Decline] when no handler is registered. */
fun interface FileChangeApprovalHandler {
    /** Evaluates the requested host file mutation and returns an explicit approval decision. */
    suspend fun decide(request: FileChangeApprovalRequest): ApprovalDecision
}
