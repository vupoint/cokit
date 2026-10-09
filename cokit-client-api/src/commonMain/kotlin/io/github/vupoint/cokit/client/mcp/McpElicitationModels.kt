package io.github.vupoint.cokit.client.mcp

import io.github.vupoint.cokit.client.CodexJsonPayload
import io.github.vupoint.cokit.client.ThreadId
import io.github.vupoint.cokit.client.TurnId
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Server-initiated MCP request for application-mediated user input. CoKit declines it unless an explicit handler is registered; request text and URLs are untrusted.
 */
@Serializable(with = McpElicitationRequestSerializer::class)
sealed interface McpElicitationRequest {
    val serverName: String
    val threadId: ThreadId
    val turnId: TurnId?
    val message: String

    /** Typed MCP form request; applications validate answers against the supplied schema before accepting. */
    @Serializable
    data class Form(
        override val serverName: String,
        override val threadId: ThreadId,
        override val turnId: TurnId? = null,
        override val message: String,
        @SerialName("_meta")
        val meta: CodexJsonPayload? = null,
        val requestedSchema: McpElicitationSchema,
    ) : McpElicitationRequest

    /** OpenAI form request whose schema remains opaque JSON, preserving server-specific field definitions. */
    @Serializable
    data class OpenAiForm(
        override val serverName: String,
        override val threadId: ThreadId,
        override val turnId: TurnId? = null,
        override val message: String,
        @SerialName("_meta")
        val meta: CodexJsonPayload? = null,
        val requestedSchema: CodexJsonPayload,
    ) : McpElicitationRequest

    /**
     * URL-based consent request. CoKit does not open the URL; the application owns destination validation and user interaction.
     */
    @Serializable
    data class Url(
        override val serverName: String,
        override val threadId: ThreadId,
        override val turnId: TurnId? = null,
        override val message: String,
        val url: String,
        val elicitationId: String,
        @SerialName("_meta")
        val meta: CodexJsonPayload? = null,
    ) : McpElicitationRequest
}

/** Selects form, OpenAI form, or URL decoding from the wire mode; unknown modes fail decoding rather than being accepted. */
object McpElicitationRequestSerializer :
    JsonContentPolymorphicSerializer<McpElicitationRequest>(McpElicitationRequest::class) {
    override fun selectDeserializer(
        element: JsonElement,
    ): DeserializationStrategy<McpElicitationRequest> {
        return when (element.jsonObject["mode"]?.jsonPrimitive?.contentOrNull) {
            "form" -> McpElicitationRequest.Form.serializer()
            "openai/form", "openaiForm" -> McpElicitationRequest.OpenAiForm.serializer()
            "url" -> McpElicitationRequest.Url.serializer()
            else -> throw SerializationException("Unknown MCP elicitation request mode")
        }
    }
}

/**
 * Object form schema supplied by an MCP server.
 *
 * @property properties Field definitions keyed by their response names.
 * @property required Optional names the server requires in an accepted response.
 */
@Serializable
data class McpElicitationSchema(
    @SerialName("\$schema")
    val schema: String? = null,
    val type: McpElicitationSchemaType,
    val properties: Map<String, McpElicitationField>,
    val required: List<String>? = null,
)

/** Object root supported by the typed MCP elicitation form schema. */
@Serializable
enum class McpElicitationSchemaType {
    @SerialName("object")
    Object,
}

/**
 * Typed MCP form field constraints and choices; applications own rendering, validation, and consent before submitting a response.
 *
 * @property default Original JSON default, which is not automatically submitted.
 * @property enum Wire values offered by the field.
 * @property enumNames Optional display labels for enum values.
 * @property oneOf Labeled constant choices for a single selection.
 * @property anyOf Labeled constant choices, including array item selections.
 * @property minLength Optional minimum string length.
 * @property maxLength Optional maximum string length.
 */
@Serializable
data class McpElicitationField(
    val type: McpElicitationFieldType,
    val title: String? = null,
    val description: String? = null,
    val default: CodexJsonPayload? = null,
    val format: String? = null,
    val enum: List<String>? = null,
    val enumNames: List<String>? = null,
    val oneOf: List<McpElicitationConstOption>? = null,
    val anyOf: List<McpElicitationConstOption>? = null,
    val items: McpElicitationArrayItems? = null,
    val minimum: Double? = null,
    val maximum: Double? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val minItems: Int? = null,
    val maxItems: Int? = null,
)

/** Scalar or array field kinds supported by the typed MCP form schema. */
@Serializable
enum class McpElicitationFieldType {
    @SerialName("string")
    String,

    @SerialName("number")
    Number,

    @SerialName("integer")
    Integer,

    @SerialName("boolean")
    Boolean,

    @SerialName("array")
    Array,
}

/** A wire constant and its display title in an MCP elicitation choice. */
@Serializable
data class McpElicitationConstOption(
    @SerialName("const")
    val value: String,
    val title: String,
)

/** Element type and permitted choices for an MCP form array field. */
@Serializable
data class McpElicitationArrayItems(
    val type: McpElicitationFieldType? = null,
    val enum: List<String>? = null,
    val anyOf: List<McpElicitationConstOption>? = null,
)

/** Explicit MCP elicitation outcome. Decline and cancel send no content; acceptance requires application-supplied content. */
sealed interface McpElicitationResponse {
    /** Submits explicitly accepted content and optional metadata to the requesting MCP server. */
    data class Accept(
        val content: CodexJsonPayload,
        val meta: CodexJsonPayload? = null,
    ) : McpElicitationResponse

    /** Refuses the request with no content; CoKit uses this when no handler is registered. */
    data object Decline : McpElicitationResponse

    /** Reports cancellation of the elicitation interaction with no content. */
    data object Cancel : McpElicitationResponse
}

/**
 * Application callback for MCP user input or URL consent. Without a handler, CoKit replies with [McpElicitationResponse.Decline] and does not open URLs.
 */
fun interface McpElicitationHandler {
    /** Handles untrusted form or URL requests and returns an explicit consent outcome. */
    suspend fun respond(request: McpElicitationRequest): McpElicitationResponse
}
