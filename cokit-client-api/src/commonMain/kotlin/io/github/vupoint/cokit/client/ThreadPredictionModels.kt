@file:OptIn(ExperimentalCodexApi::class)

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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/** Boolean wire flag whose construction requires Kotlin opt-in, including in stable fork signatures. */
@Serializable
@JvmInline
value class ThreadPredictionMode @ExperimentalCodexApi constructor(val enabled: Boolean)

/** Requests a prediction; the response acknowledges dispatch and the update notification carries its result. */
@ExperimentalCodexApi
@Serializable
data class ThreadPredictionRequestParams(val threadId: ThreadId, val sourceTurnId: TurnId)

/** Prediction outcome, including unknown future variants. */
@ExperimentalCodexApi
@Serializable(with = ThreadPredictionResultSerializer::class)
sealed interface ThreadPredictionResult {
    /** Completed prediction; a null [text] is a valid outcome. */
    @Serializable
    data class Completed(val text: String? = null) : ThreadPredictionResult

    /** Prediction failed without a text result. */
    data object Failed : ThreadPredictionResult

    /** Future upstream result retained without interpretation. */
    data class Unknown(val payload: CodexJsonPayload) : ThreadPredictionResult
}

/** JSON serializer for prediction outcomes and opaque future variants. */
@ExperimentalCodexApi
object ThreadPredictionResultSerializer : KSerializer<ThreadPredictionResult> {
    override val descriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): ThreadPredictionResult {
        val json = decoder as? JsonDecoder ?: throw SerializationException("Predictions require JSON")
        val value = json.decodeJsonElement() as? JsonObject ?: throw SerializationException("Expected a prediction object")
        val type = value["type"] as? JsonPrimitive
        if (type == null || !type.isString) throw SerializationException("Expected a prediction type")
        return when (type.content) {
            "completed" -> {
                val text = value["text"]
                if (text != null && text != JsonNull && (text !is JsonPrimitive || !text.isString)) {
                    throw SerializationException("Expected prediction text or null")
                }
                json.json.decodeFromJsonElement<ThreadPredictionResult.Completed>(value)
            }
            "failed" -> ThreadPredictionResult.Failed
            else -> ThreadPredictionResult.Unknown(value.toCodexPayload())
        }
    }

    override fun serialize(encoder: Encoder, value: ThreadPredictionResult) {
        val json = encoder as? JsonEncoder ?: throw SerializationException("Predictions require JSON")
        json.encodeJsonElement(when (value) {
            is ThreadPredictionResult.Completed -> JsonObject(json.json.encodeToJsonElement(value).jsonObject + ("type" to JsonPrimitive("completed")))
            ThreadPredictionResult.Failed -> buildJsonObject { put("type", "failed") }
            is ThreadPredictionResult.Unknown -> value.payload.toJsonElement()!!
        })
    }
}
