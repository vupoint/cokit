@file:OptIn(ExperimentalCodexApi::class)

package io.github.vupoint.cokit.client

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
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
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/** Opaque equality token scoped to one thread and store; never sort it or reuse it across threads. */
@ExperimentalCodexApi
@Serializable
@JvmInline
value class ThreadReadStateRevision(val value: String)

/** Durable read receipt; null [firstUnread] means read, while an absent receipt means unavailable. */
@Serializable
data class ThreadReadState @ExperimentalCodexApi constructor(
    val revision: ThreadReadStateRevision,
    val firstUnread: ThreadUnreadPosition? = null,
)

/** Earliest unread result or an explicit unread mark, with future variants preserved. */
@ExperimentalCodexApi
@Serializable(with = ThreadUnreadPositionSerializer::class)
sealed interface ThreadUnreadPosition {
    /** Explicit unread mark, including on an empty thread. */
    data object ThreadStart : ThreadUnreadPosition

    /** Earliest eligible completed result that has not been acknowledged. */
    @Serializable
    data class Turn(val turnId: TurnId) : ThreadUnreadPosition

    /** Future server position retained without interpretation. */
    data class Unknown(val payload: CodexJsonPayload) : ThreadUnreadPosition
}

/** JSON serializer for typed read positions and opaque future variants. */
@ExperimentalCodexApi
object ThreadUnreadPositionSerializer : KSerializer<ThreadUnreadPosition> {
    override val descriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): ThreadUnreadPosition {
        val json = decoder as? JsonDecoder ?: throw SerializationException("Read positions require JSON")
        val value = json.decodeJsonElement() as? JsonObject ?: throw SerializationException("Expected a read position object")
        val type = value["type"] as? JsonPrimitive
        if (type == null || !type.isString) throw SerializationException("Expected a read position type")
        return when (type.content) {
            "threadStart" -> ThreadUnreadPosition.ThreadStart
            "turn" -> {
                val turnId = value["turnId"] as? JsonPrimitive
                if (turnId == null || !turnId.isString) throw SerializationException("Expected a turn ID string")
                json.json.decodeFromJsonElement<ThreadUnreadPosition.Turn>(value)
            }
            else -> ThreadUnreadPosition.Unknown(value.toCodexPayload())
        }
    }

    override fun serialize(encoder: Encoder, value: ThreadUnreadPosition) {
        val json = encoder as? JsonEncoder ?: throw SerializationException("Read positions require JSON")
        json.encodeJsonElement(when (value) {
            ThreadUnreadPosition.ThreadStart -> buildJsonObject { put("type", "threadStart") }
            is ThreadUnreadPosition.Turn -> JsonObject(json.json.encodeToJsonElement(value).jsonObject + ("type" to JsonPrimitive("turn")))
            is ThreadUnreadPosition.Unknown -> value.payload.toJsonElement()!!
        })
    }
}

/** Explicit receipt edit; reading a snapshot alone never acknowledges activity. */
@ExperimentalCodexApi
@Serializable
sealed interface ThreadReadStateOperation {
    /** Clears the unread position only if the observed revision still matches. */
    @Serializable
    @SerialName("read")
    data object Read : ThreadReadStateOperation

    /** Marks the thread unread from its start only if the observed revision still matches. */
    @Serializable
    @SerialName("unread")
    data object Unread : ThreadReadStateOperation
}

/** Compare-and-set receipt edit. Conflicts require a fresh user action, never a blind retry. */
@ExperimentalCodexApi
@Serializable
data class ThreadReadStateUpdateParams(
    val threadId: ThreadId,
    val expectedRevision: ThreadReadStateRevision,
    val operation: ThreadReadStateOperation,
)

/** Receipt returned after an accepted edit. */
@ExperimentalCodexApi
@Serializable
data class ThreadReadStateUpdateResult(val readState: ThreadReadState)
