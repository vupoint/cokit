@file:OptIn(io.github.vupoint.cokit.client.ExperimentalCodexApi::class)

package io.github.vupoint.cokit.client.tools

import io.github.vupoint.cokit.client.CodexJsonPayload
import io.github.vupoint.cokit.client.ExperimentalCodexApi
import io.github.vupoint.cokit.client.ThreadId
import io.github.vupoint.cokit.client.TurnId
import io.github.vupoint.cokit.client.toCodexPayload
import io.github.vupoint.cokit.client.toJsonElement
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
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/** Client-owned tools advertised when starting a thread. Registration does not authorize execution. */
@Serializable
sealed interface DynamicToolSpec {
    /**
     * Experimental function advertised to app-server. The input schema is preserved as JSON and execution still requires an explicit handler.
     *
     * @property deferLoading Leaves discovery/loading to app-server; CoKit does not implement tool search.
     */
    @ExperimentalCodexApi
    @Serializable
    @SerialName("function")
    data class Function(
        val name: String,
        val description: String,
        val inputSchema: CodexJsonPayload,
        val deferLoading: Boolean = false,
    ) : DynamicToolSpec

    /** Experimental group of function tools; nested namespaces are not supported by the upstream schema. */
    @ExperimentalCodexApi
    @Serializable
    @SerialName("namespace")
    data class Namespace(
        val name: String,
        val description: String,
        val tools: List<DynamicToolNamespaceTool>,
    ) : DynamicToolSpec
}

/** The upstream namespace schema permits functions, but not nested namespaces. */
@ExperimentalCodexApi
@Serializable
sealed interface DynamicToolNamespaceTool {
    /**
     * Experimental function advertised to app-server. The input schema is preserved as JSON and execution still requires an explicit handler.
     *
     * @property deferLoading Leaves discovery/loading to app-server; CoKit does not implement tool search.
     */
    @ExperimentalCodexApi
    @Serializable
    @SerialName("function")
    data class Function(
        val name: String,
        val description: String,
        val inputSchema: CodexJsonPayload,
        val deferLoading: Boolean = false,
    ) : DynamicToolNamespaceTool
}

/** Untrusted tool arguments; [callId] identifies the tool call, not the JSON-RPC request. */
@ExperimentalCodexApi
@Serializable
data class DynamicToolCallRequest(
    val threadId: ThreadId,
    val turnId: TurnId,
    val callId: String,
    val tool: String,
    val arguments: CodexJsonPayload,
    val namespace: String? = null,
) {
    override fun toString(): String = "DynamicToolCallRequest(threadId=$threadId, turnId=$turnId, callId=$callId, arguments=<redacted>)"
}

/** An explicit outcome. Empty content and success=false is a refusal without execution. */
@ExperimentalCodexApi
@Serializable
data class DynamicToolCallResponse(
    val contentItems: List<DynamicToolCallOutputContent>,
    val success: Boolean,
) {
    override fun toString(): String = "DynamicToolCallResponse(contentItemCount=${contentItems.size}, success=$success)"
}

/**
 * Runs in the application's environment; implementations own validation, authorization and confirmation.
 * Without a registered handler, CoKit returns empty content with success=false and executes nothing.
 */
@ExperimentalCodexApi
fun interface DynamicToolCallHandler {
    /** Validates and authorizes untrusted arguments before running application-owned behavior and reporting its outcome. */
    suspend fun call(request: DynamicToolCallRequest): DynamicToolCallResponse
}

/** Dynamic tool results use camelCase wire types, unlike turn tool outputs. */
@Serializable(with = DynamicToolCallOutputContentSerializer::class)
sealed interface DynamicToolCallOutputContent {
    /** Text result using the dynamic-tool inputText wire tag. */
    @ExperimentalCodexApi
    @Serializable
    data class Text(val text: String) : DynamicToolCallOutputContent {
        override fun toString(): String = "Text(<redacted>)"
    }

    /** Image reference using the dynamic-tool inputImage wire tag; CoKit does not fetch or render it. */
    @ExperimentalCodexApi
    @Serializable
    data class Image(val imageUrl: String) : DynamicToolCallOutputContent {
        override fun toString(): String = "Image(<redacted>)"
    }

    /** Audio reference using the dynamic-tool inputAudio wire tag; CoKit does not fetch or play it. */
    @ExperimentalCodexApi
    @Serializable
    data class Audio(val audioUrl: String) : DynamicToolCallOutputContent {
        override fun toString(): String = "Audio(<redacted>)"
    }

    /** Preserves future upstream content variants without interpreting them. */
    @ExperimentalCodexApi
    data class Unknown(val payload: CodexJsonPayload) : DynamicToolCallOutputContent {
        override fun toString(): String = "Unknown(<redacted>)"
    }
}

/**
 * JSON-only serializer using camelCase dynamic-tool content tags and preserving unknown variants without interpreting their payloads.
 */
@ExperimentalCodexApi
object DynamicToolCallOutputContentSerializer : KSerializer<DynamicToolCallOutputContent> {
    override val descriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): DynamicToolCallOutputContent {
        val input = decoder as? JsonDecoder ?: throw SerializationException("Dynamic tool content requires JSON")
        val element = input.decodeJsonElement()
        return when ((element as? JsonObject)?.get("type")) {
            JsonPrimitive("inputText") -> input.json.decodeFromJsonElement<DynamicToolCallOutputContent.Text>(element)
            JsonPrimitive("inputImage") -> input.json.decodeFromJsonElement<DynamicToolCallOutputContent.Image>(element)
            JsonPrimitive("inputAudio") -> input.json.decodeFromJsonElement<DynamicToolCallOutputContent.Audio>(element)
            else -> DynamicToolCallOutputContent.Unknown(element.toCodexPayload())
        }
    }

    override fun serialize(encoder: Encoder, value: DynamicToolCallOutputContent) {
        val output = encoder as? JsonEncoder ?: throw SerializationException("Dynamic tool content requires JSON")
        val (type, content) = when (value) {
            is DynamicToolCallOutputContent.Text -> "inputText" to output.json.encodeToJsonElement(value)
            is DynamicToolCallOutputContent.Image -> "inputImage" to output.json.encodeToJsonElement(value)
            is DynamicToolCallOutputContent.Audio -> "inputAudio" to output.json.encodeToJsonElement(value)
            is DynamicToolCallOutputContent.Unknown -> {
                output.encodeJsonElement(requireNotNull(value.payload.toJsonElement()))
                return
            }
        }
        output.encodeJsonElement(JsonObject(content.jsonObject + ("type" to JsonPrimitive(type))))
    }
}
