package io.github.vupoint.cokit.client

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Request cursor for item history; response cursors remain [CodexCursor] strings. */
@Serializable(with = ThreadItemsListCursorSerializer::class)
sealed interface ThreadItemsListCursor {
    /** Reuses a response pagination token without interpreting its contents. */
    data class Opaque(val cursor: CodexCursor) : ThreadItemsListCursor

    /** Exclusive position within the turn selected by [ThreadItemsListParams.turnId]. */
    data class ItemAnchor(val itemId: ItemId) : ThreadItemsListCursor {
        init {
            require(itemId.value.isNotBlank()) { "itemId must not be blank" }
        }
    }
}

/** JSON serializer for opaque string cursors and structured exclusive item anchors. */
object ThreadItemsListCursorSerializer : KSerializer<ThreadItemsListCursor> {
    override val descriptor = JsonElement.serializer().descriptor

    override fun serialize(encoder: Encoder, value: ThreadItemsListCursor) {
        val json = encoder as? JsonEncoder ?: throw SerializationException("Item history cursors require JSON")
        json.encodeJsonElement(when (value) {
            is ThreadItemsListCursor.Opaque -> JsonPrimitive(value.cursor.value)
            is ThreadItemsListCursor.ItemAnchor -> buildJsonObject {
                put("type", "item")
                put("itemId", value.itemId.value)
            }
        })
    }

    override fun deserialize(decoder: Decoder): ThreadItemsListCursor {
        val json = decoder as? JsonDecoder ?: throw SerializationException("Item history cursors require JSON")
        val value = json.decodeJsonElement()
        if (value is JsonPrimitive && value.isString) {
            return ThreadItemsListCursor.Opaque(CodexCursor(value.content))
        }
        if (value is JsonObject && value["type"] == JsonPrimitive("item")) {
            val itemId = value["itemId"] as? JsonPrimitive
            if (itemId != null && itemId.isString && itemId.content.isNotBlank()) {
                return ThreadItemsListCursor.ItemAnchor(ItemId(itemId.content))
            }
        }
        throw SerializationException("Expected an opaque string cursor or an item anchor with a non-blank itemId")
    }
}

/**
 * Pagination for raw thread-item history.
 *
 * A [ThreadItemsListCursor.ItemAnchor] requires a non-blank [turnId] selecting its turn.
 * Null pagination and ordering options are omitted and use server defaults.
 */
@Serializable
data class ThreadItemsListParams(
    val threadId: ThreadId,
    val turnId: TurnId? = null,
    val cursor: ThreadItemsListCursor? = null,
    val limit: Int? = null,
    val sortDirection: SortDirection? = null,
) {
    init {
        require(cursor !is ThreadItemsListCursor.ItemAnchor || !turnId?.value.isNullOrBlank()) {
            "turnId is required when cursor is an item anchor"
        }
    }
}

/**
 * Raw history item paired with its owning turn.
 *
 * [item] retains the complete JSON item, including unknown types and fields.
 *
 * @property startedAtMs Optional item start timestamp in Unix milliseconds.
 * @property completedAtMs Optional item completion timestamp in Unix milliseconds.
 */
@Serializable
data class ThreadItemEntry(
    val turnId: TurnId,
    val item: CodexJsonPayload,
    val startedAtMs: Long? = null,
    val completedAtMs: Long? = null,
)

/** Page of raw history items with opaque continuation tokens for either direction. */
@Serializable
data class ThreadItemsListResult(
    val data: List<ThreadItemEntry>,
    val nextCursor: CodexCursor? = null,
    val backwardsCursor: CodexCursor? = null,
)

/** Replaces paginated conversation history before a turn; does not revert filesystem changes. */
@Serializable
data class ThreadRevertParams(val threadId: ThreadId, val beforeTurnId: TurnId)

/** Thread snapshot and pagination cursors after conversation history was reverted. */
@Serializable
data class ThreadRevertResult(
    val thread: Thread,
    val itemsBackwardsCursor: CodexCursor? = null,
    val turnsBackwardsCursor: CodexCursor? = null,
)

/** History representation reported by the server: legacy inline history or paginated history. */
@Serializable
@JvmInline
value class ThreadHistoryMode(val value: String) {
    companion object {
        val Legacy = ThreadHistoryMode("legacy")
        val Paginated = ThreadHistoryMode("paginated")
    }
}

/** Thread runtime state and optional active-state flags such as waiting for approval. */
@Serializable
data class ThreadStatus(val type: ThreadStatusType, val activeFlags: List<ThreadActiveFlag>? = null)

/** Additional reason an active thread is waiting; unknown string values remain representable. */
@Serializable
@JvmInline
value class ThreadActiveFlag(val value: String) {
    companion object {
        val WaitingOnApproval = ThreadActiveFlag("waitingOnApproval")
        val WaitingOnUserInput = ThreadActiveFlag("waitingOnUserInput")
    }
}

/** Server collaboration mode and its effective model and instruction settings. */
@Serializable
data class ThreadCollaborationMode(
    val mode: io.github.vupoint.cokit.client.environment.CollaborationModeKind,
    val settings: ThreadCollaborationSettings,
)

/** Model settings included in a collaboration-mode snapshot. Null fields have no reported value. */
@Serializable
data class ThreadCollaborationSettings(
    val model: ModelName,
    @kotlinx.serialization.SerialName("reasoning_effort") val reasoningEffort: ReasoningEffort? = null,
    @kotlinx.serialization.SerialName("developer_instructions") val developerInstructions: String? = null,
)
