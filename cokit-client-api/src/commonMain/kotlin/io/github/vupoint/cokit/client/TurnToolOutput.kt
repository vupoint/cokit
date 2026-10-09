package io.github.vupoint.cokit.client

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

/** Tool result supplied as turn input, identified by [name] and optional [namespace]. */
@Serializable
data class TurnToolOutput(val name: String, val output: ToolOutputBody, val namespace: String? = null)

/** Tool-result body encoded as either a JSON string or an array of typed content elements. */
@Serializable(with = ToolOutputBodySerializer::class)
sealed interface ToolOutputBody {
    /** Plain tool-result text encoded as a JSON string. */
    data class Text(val text: String) : ToolOutputBody
    /** Ordered multimodal tool-result elements encoded as a JSON array. */
    data class Content(val items: List<ToolOutputContent>) : ToolOutputBody
}

/** One element of a multimodal tool-result body. */
@Serializable
sealed interface ToolOutputContent {
    /** Text element inside a multimodal tool-result content array. */
    @Serializable
    @SerialName("input_text")
    data class Text(val text: String) : ToolOutputContent

    /** Image tool-result element. Exactly one of [imageUrl] and [fileId] must be supplied. */
    @Serializable
    @SerialName("input_image")
    data class Image(
        @SerialName("image_url") val imageUrl: String? = null,
        @SerialName("file_id") val fileId: String? = null,
        val detail: String? = null,
    ) : ToolOutputContent {
        init { require((imageUrl != null) != (fileId != null)) { "Provide exactly one image URL or file ID" } }
    }

    /** Audio tool-result element referenced by [audioUrl]. */
    @Serializable
    @SerialName("input_audio")
    data class Audio(@SerialName("audio_url") val audioUrl: String) : ToolOutputContent

    /** Opaque encrypted tool-result content, passed through without decryption. */
    @Serializable
    @SerialName("encrypted_content")
    data class Encrypted(@SerialName("encrypted_content") val encryptedContent: String) : ToolOutputContent
}

/** JSON serializer accepting only tool-result strings or typed content arrays. */
object ToolOutputBodySerializer : KSerializer<ToolOutputBody> {
    override val descriptor = JsonElement.serializer().descriptor
    override fun serialize(encoder: Encoder, value: ToolOutputBody) {
        val json = encoder as? JsonEncoder ?: throw SerializationException("ToolOutputBody requires JSON")
        json.encodeJsonElement(when (value) {
            is ToolOutputBody.Text -> JsonPrimitive(value.text)
            is ToolOutputBody.Content -> json.json.encodeToJsonElement(ListSerializer(ToolOutputContent.serializer()), value.items)
        })
    }
    override fun deserialize(decoder: Decoder): ToolOutputBody {
        val json = decoder as? JsonDecoder ?: throw SerializationException("ToolOutputBody requires JSON")
        return when (val value = json.decodeJsonElement()) {
            is JsonPrimitive -> if (value.isString) ToolOutputBody.Text(value.content) else throw SerializationException("Expected tool output text")
            is JsonArray -> ToolOutputBody.Content(json.json.decodeFromJsonElement(ListSerializer(ToolOutputContent.serializer()), value))
            else -> throw SerializationException("Expected tool output text or content array")
        }
    }
}
