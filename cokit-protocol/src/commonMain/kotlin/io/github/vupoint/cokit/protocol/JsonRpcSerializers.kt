package io.github.vupoint.cokit.protocol

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

/**
 * JSON-only serializer for app-server request, notification, and response envelopes.
 *
 * Decoding requires an object with a recognized field combination. Requests contain
 * `id` and `method`, responses contain `id` and either `result` or `error`, and
 * notifications contain `method` without `id`. Unknown-field handling follows the
 * supplied [Json] configuration; unknown envelope fields are not retained.
 */
object JsonRpcMessageSerializer : KSerializer<JsonRpcMessage> {
    /** Descriptor for the polymorphic envelope encoded by this serializer. */
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("JsonRpcMessage")

    /**
     * Decodes an envelope using the decoder's JSON configuration.
     *
     * @throws SerializationException If decoding is not JSON, the shape is unrecognized,
     * or the selected envelope's fields are invalid.
     */
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
                if ("result" in obj && "error" in obj) {
                    throw SerializationException("JSON-RPC response must not contain both result and error")
                }
                jsonDecoder.json.decodeFromJsonElement<JsonRpcResponse>(obj)
            }
            "method" in obj -> {
                jsonDecoder.json.decodeFromJsonElement<JsonRpcNotification>(obj)
            }
            else -> throw SerializationException("Unrecognized JSON-RPC message shape")
        }
    }

    /**
     * Encodes the concrete envelope without adding a `jsonrpc` field.
     *
     * @throws SerializationException If the encoder does not support JSON.
     */
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

/** JSON-only serializer preserving string identifiers and signed 64-bit integer identifiers. */
object JsonRpcIdSerializer : KSerializer<JsonRpcId> {
    /** Logical descriptor; numeric identifiers are still encoded as JSON numbers. */
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("JsonRpcId", PrimitiveKind.STRING)

    /**
     * Decodes strings as [JsonRpcId.StringId] and integers as [JsonRpcId.Number].
     *
     * @throws SerializationException For non-JSON decoding, other JSON kinds, or
     * numeric values that cannot be represented as a [Long].
     */
    override fun deserialize(decoder: Decoder): JsonRpcId {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("JsonRpcId requires JSON decoding")
        val element = jsonDecoder.decodeJsonElement()
        val primitive = element as? JsonPrimitive
            ?: throw SerializationException("JSON-RPC id must be a string or number")

        return if (primitive.toString().startsWith("\"")) {
            JsonRpcId.StringId(primitive.content)
        } else {
            primitive.longOrNull?.let(JsonRpcId::Number)
                ?: throw SerializationException("JSON-RPC numeric id must fit in Long")
        }
    }

    /**
     * Encodes the identifier using its original JSON kind.
     *
     * @throws SerializationException If the encoder does not support JSON.
     */
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

/**
 * Shared protocol JSON configuration that ignores unknown fields in typed envelopes.
 *
 * Raw parameters, results, and error data remain [JsonElement] values. Other serialization
 * settings retain their [Json] defaults, including omission of properties with default values.
 */
val CodexProtocolJson: Json = Json {
    ignoreUnknownKeys = true
}
