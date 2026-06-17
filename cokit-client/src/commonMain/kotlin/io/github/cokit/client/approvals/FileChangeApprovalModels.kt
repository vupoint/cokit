package io.github.cokit.client.approvals

import io.github.cokit.client.CodexHostPath
import io.github.cokit.client.ItemId
import io.github.cokit.client.ThreadId
import io.github.cokit.client.TurnId
import kotlinx.serialization.Serializable

@Serializable
data class FileChangeApprovalRequest(
    val threadId: ThreadId,
    val turnId: TurnId,
    val itemId: ItemId,
    val startedAtMs: Long,
    val reason: String? = null,
    val grantRoot: CodexHostPath? = null,
)

@Serializable
data class FileChangeSummary(
    val path: CodexHostPath,
    val kind: FileChangeKind,
    val diff: String? = null,
)

@Serializable
@JvmInline
value class FileChangeKind(val value: String)

fun interface FileChangeApprovalHandler {
    suspend fun decide(request: FileChangeApprovalRequest): ApprovalDecision
}
