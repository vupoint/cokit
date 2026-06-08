package io.github.cokit.client

import io.github.cokit.protocol.CodexProtocolJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder

@Serializable(with = CodexJsonPayloadSerializer::class)
class CodexJsonPayload internal constructor(
    internal val element: JsonElement,
) {
    fun toJsonString(): String = CodexProtocolJson.encodeToString(JsonElement.serializer(), element)

    override fun equals(other: Any?): Boolean =
        other is CodexJsonPayload && element == other.element

    override fun hashCode(): Int = element.hashCode()

    override fun toString(): String = toJsonString()

    companion object {
        fun parse(json: String): CodexJsonPayload =
            CodexJsonPayload(CodexProtocolJson.parseToJsonElement(json))
    }
}

object CodexJsonPayloadSerializer : KSerializer<CodexJsonPayload> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): CodexJsonPayload {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("CodexJsonPayload requires JSON decoding")
        return CodexJsonPayload(jsonDecoder.decodeJsonElement())
    }

    override fun serialize(encoder: Encoder, value: CodexJsonPayload) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("CodexJsonPayload requires JSON encoding")
        jsonEncoder.encodeJsonElement(value.element)
    }
}

internal fun JsonElement.toCodexPayload(): CodexJsonPayload = CodexJsonPayload(this)

internal fun CodexJsonPayload?.toJsonElement(): JsonElement? = this?.element
