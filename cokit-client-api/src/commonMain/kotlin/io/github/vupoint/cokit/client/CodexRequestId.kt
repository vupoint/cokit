package io.github.vupoint.cokit.client

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Outbound JSON-RPC request identity, for operations that cancel a specific request. */
@Serializable(with = CodexRequestIdSerializer::class)
sealed interface CodexRequestId {
    /** Preserves an integer JSON-RPC request ID without converting it to a string. */
    data class Number(val value: Long) : CodexRequestId
    /** Preserves a string JSON-RPC request ID without numeric coercion. */
    data class StringId(val value: String) : CodexRequestId
}

/** JSON serializer that retains the numeric or string form of an outbound request ID. */
object CodexRequestIdSerializer : KSerializer<CodexRequestId> {
    override val descriptor = PrimitiveSerialDescriptor("CodexRequestId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CodexRequestId) {
        val element = when (value) {
            is CodexRequestId.Number -> JsonPrimitive(value.value)
            is CodexRequestId.StringId -> JsonPrimitive(value.value)
        }
        (encoder as JsonEncoder).encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): CodexRequestId {
        val element = (decoder as JsonDecoder).decodeJsonElement() as? JsonPrimitive
            ?: throw SerializationException("Expected a numeric or string request ID")
        return if (element.isString) CodexRequestId.StringId(element.content)
        else CodexRequestId.Number(element.longOrNull
            ?: throw SerializationException("Expected an integer request ID"))
    }
}
