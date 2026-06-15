package io.github.cokit.client

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ItemId(val value: String)

@Serializable
@JvmInline
value class ItemType(val value: String) {
    companion object {
        val UserMessage = ItemType("userMessage")
        val AgentMessage = ItemType("agentMessage")
        val Plan = ItemType("plan")
        val Reasoning = ItemType("reasoning")
        val CommandExecution = ItemType("commandExecution")
        val FileChange = ItemType("fileChange")
        val McpToolCall = ItemType("mcpToolCall")
        val CollabToolCall = ItemType("collabToolCall")
        val WebSearch = ItemType("webSearch")
        val ImageView = ItemType("imageView")
        val EnteredReviewMode = ItemType("enteredReviewMode")
        val ExitedReviewMode = ItemType("exitedReviewMode")
        val ContextCompaction = ItemType("contextCompaction")
        val Compacted = ItemType("compacted")
    }
}

@Serializable
@JvmInline
value class ItemStatus(val value: String) {
    companion object {
        val InProgress = ItemStatus("inProgress")
        val Completed = ItemStatus("completed")
        val Failed = ItemStatus("failed")
        val Declined = ItemStatus("declined")
    }
}

@Serializable
data class ThreadItemSummary(
    val id: ItemId,
    val type: ItemType,
    val status: ItemStatus? = null,
    val text: String? = null,
    val command: String? = null,
    val cwd: CodexHostPath? = null,
    val query: String? = null,
    val path: CodexHostPath? = null,
    val review: String? = null,
    val aggregatedOutput: String? = null,
    val exitCode: Int? = null,
    val durationMs: Long? = null,
)
