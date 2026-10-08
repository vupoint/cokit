package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.tools.DynamicToolCallOutputContent
import io.github.vupoint.cokit.client.mcp.McpAppUi
import io.github.vupoint.cokit.client.mcp.McpResourceUri
import kotlinx.serialization.Serializable

/** Opaque identifier for an item emitted within a turn. */
@Serializable
@JvmInline
value class ItemId(val value: String)

/** Server item discriminator. Unknown strings remain representable without defining a typed item. */
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
        @ExperimentalCodexApi
        val DynamicToolCall = ItemType("dynamicToolCall")
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

/** Item execution state, including failure and explicit decline. */
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

/**
 * Typed projection used by item lifecycle notifications.
 *
 * Only fields relevant to the item kind are populated. This projection does not retain every raw
 * item field; use thread-item history or [Turn.items] for complete JSON payloads.
 *
 * @property durationMs Optional execution duration in milliseconds.
 * @property exitCode Optional process exit code for command items.
 */
@OptIn(ExperimentalCodexApi::class)
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
    @ExperimentalCodexApi val tool: String? = null,
    @ExperimentalCodexApi val namespace: String? = null,
    @ExperimentalCodexApi val arguments: CodexJsonPayload? = null,
    @ExperimentalCodexApi val contentItems: List<DynamicToolCallOutputContent>? = null,
    @ExperimentalCodexApi val success: Boolean? = null,
    /** Explicit upstream presentation preference; absence does not imply inline display. */
    val mcpAppUi: McpAppUi? = null,
    /** Legacy URI retained even when no supported display preference was supplied. */
    val mcpAppResourceUri: McpResourceUri? = null,
)
