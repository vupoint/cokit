package io.github.cokit.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.longOrNull

object JsonRpcMessageSerializer : KSerializer<JsonRpcMessage> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("JsonRpcMessage")

    override fun deserialize(decoder: Decoder): JsonRpcMessage {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("JsonRpcMessage requires JSON decoding")
        val element = jsonDecoder.decodeJsonElement()
        val obj = element as? JsonObject
            ?: throw SerializationException("JSON-RPC message must be an object")

        return when {
            "id" in obj && "method" in obj -> {
                jsonDecoder.json.decodeFromJsonElement<JsonRpcRequest>(obj)
            }
            "id" in obj && ("result" in obj || "error" in obj) -> {
                jsonDecoder.json.decodeFromJsonElement<JsonRpcResponse>(obj)
            }
            "method" in obj -> {
                jsonDecoder.json.decodeFromJsonElement<JsonRpcNotification>(obj)
            }
            else -> throw SerializationException("Unrecognized JSON-RPC message shape")
        }
    }

    override fun serialize(encoder: Encoder, value: JsonRpcMessage) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("JsonRpcMessage requires JSON encoding")
        val element = when (value) {
            is JsonRpcRequest -> jsonEncoder.json.encodeToJsonElement(value)
            is JsonRpcNotification -> jsonEncoder.json.encodeToJsonElement(value)
            is JsonRpcResponse -> jsonEncoder.json.encodeToJsonElement(value)
        }
        jsonEncoder.encodeJsonElement(element)
    }
}

object JsonRpcIdSerializer : KSerializer<JsonRpcId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("JsonRpcId", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): JsonRpcId {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("JsonRpcId requires JSON decoding")
        val element = jsonDecoder.decodeJsonElement()
        val primitive = element as? JsonPrimitive
            ?: throw SerializationException("JSON-RPC id must be a string or number")

        return primitive.longOrNull?.let(JsonRpcId::Number)
            ?: JsonRpcId.StringId(primitive.content)
    }

    override fun serialize(encoder: Encoder, value: JsonRpcId) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("JsonRpcId requires JSON encoding")
        val element = when (value) {
            is JsonRpcId.Number -> JsonPrimitive(value.value)
            is JsonRpcId.StringId -> JsonPrimitive(value.value)
        }
        jsonEncoder.encodeJsonElement(element)
    }
}

val CodexProtocolJson: Json = Json {
    ignoreUnknownKeys = true
}
