package io.github.vupoint.cokit.client

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * One input element submitted to a turn.
 *
 * Known variants have typed fields; [Custom] retains JSON for variants CoKit does not recognize.
 */
@Serializable(with = TurnInputSerializer::class)
sealed interface TurnInput {
    /** Plain text included in the user input. */
    data class Text(val text: String) : TurnInput

    /** Image referenced by a URL understood by app-server. */
    data class Image(val url: String) : TurnInput

    /** Image file path on the app-server host; CoKit does not upload the client filesystem file. */
    data class LocalImage(val path: String) : TurnInput

    /** Named skill reference whose [path] is interpreted by app-server. */
    data class Skill(
        val name: String,
        val path: String,
    ) : TurnInput

    /** Named resource reference whose [path] is interpreted by app-server. */
    data class Mention(
        val name: String,
        val path: String,
    ) : TurnInput

    /** Raw input payload preserved when no typed variant matches, or supplied for protocol extensions. */
    data class Custom(val payload: CodexJsonPayload) : TurnInput
}

/**
 * JSON serializer for typed inputs and raw fallback payloads.
 *
 * Unknown variants and recognized variants missing required fields decode as [TurnInput.Custom].
 * Additional fields on recognized typed variants are not retained by those variants.
 */
object TurnInputSerializer : KSerializer<TurnInput> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): TurnInput {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("TurnInput requires JSON decoding")
        val element = jsonDecoder.decodeJsonElement()
        val obj = element as? JsonObject ?: return TurnInput.Custom(element.toCodexPayload())

        return when (obj["type"]?.jsonPrimitive?.contentOrNull) {
            "text" -> obj["text"]?.jsonPrimitive?.contentOrNull
                ?.let(TurnInput::Text)
                ?: TurnInput.Custom(element.toCodexPayload())
            "image" -> obj["url"]?.jsonPrimitive?.contentOrNull
                ?.let(TurnInput::Image)
                ?: TurnInput.Custom(element.toCodexPayload())
            "localImage" -> obj["path"]?.jsonPrimitive?.contentOrNull
                ?.let(TurnInput::LocalImage)
                ?: TurnInput.Custom(element.toCodexPayload())
            "skill" -> {
                val name = obj["name"]?.jsonPrimitive?.contentOrNull
                val path = obj["path"]?.jsonPrimitive?.contentOrNull
                if (name != null && path != null) {
                    TurnInput.Skill(name = name, path = path)
                } else {
                    TurnInput.Custom(element.toCodexPayload())
                }
            }
            "mention" -> {
                val name = obj["name"]?.jsonPrimitive?.contentOrNull
                val path = obj["path"]?.jsonPrimitive?.contentOrNull
                if (name != null && path != null) {
                    TurnInput.Mention(name = name, path = path)
                } else {
                    TurnInput.Custom(element.toCodexPayload())
                }
            }
            else -> TurnInput.Custom(element.toCodexPayload())
        }
    }

    override fun serialize(encoder: Encoder, value: TurnInput) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("TurnInput requires JSON encoding")
        val element = when (value) {
            is TurnInput.Text -> buildJsonObject {
                put("type", "text")
                put("text", value.text)
            }
            is TurnInput.Image -> buildJsonObject {
                put("type", "image")
                put("url", value.url)
            }
            is TurnInput.LocalImage -> buildJsonObject {
                put("type", "localImage")
                put("path", value.path)
            }
            is TurnInput.Skill -> buildJsonObject {
                put("type", "skill")
                put("name", value.name)
                put("path", value.path)
            }
            is TurnInput.Mention -> buildJsonObject {
                put("type", "mention")
                put("name", value.name)
                put("path", value.path)
            }
            is TurnInput.Custom -> value.payload.element
        }

        jsonEncoder.encodeJsonElement(element)
    }
}
