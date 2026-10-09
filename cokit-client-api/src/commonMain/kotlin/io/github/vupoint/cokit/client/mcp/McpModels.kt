package io.github.vupoint.cokit.client.mcp

import io.github.vupoint.cokit.client.CodexCursor
import io.github.vupoint.cokit.client.CodexJsonPayload
import io.github.vupoint.cokit.client.ThreadId
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

/** Configured MCP server selector understood by app-server. */
@Serializable
@JvmInline
value class McpServerName(val value: String)

/** Resource identifier supplied by an MCP server; it need not identify a local file or an HTTP URL. */
@Serializable
@JvmInline
value class McpResourceUri(val value: String)

/** Tool name within a selected MCP server, separate from a client-owned dynamic tool name. */
@Serializable
@JvmInline
value class McpToolName(val value: String)

/** Server-reported MCP authentication mode or readiness; unknown strings are retained. */
@Serializable
@JvmInline
value class McpAuthStatus(val value: String) {
    companion object {
        val Unsupported = McpAuthStatus("unsupported")
        val NotLoggedIn = McpAuthStatus("notLoggedIn")
        val BearerToken = McpAuthStatus("bearerToken")
        val OAuth = McpAuthStatus("oAuth")
    }
}

/** Selects full discovery or tools-and-auth-only status data; unknown wire strings are retained. */
@Serializable
@JvmInline
value class McpServerStatusDetail(val value: String) {
    companion object {
        val Full = McpServerStatusDetail("full")
        val ToolsAndAuthOnly = McpServerStatusDetail("toolsAndAuthOnly")
    }
}

/**
 * Starts MCP OAuth through app-server; the application owns user consent and opening the resulting URL.
 *
 * @property scopes Optional requested OAuth scopes.
 * @property timeoutSecs Optional login timeout in seconds.
 * @property threadId Optional thread-specific MCP connection context.
 */
@Serializable
data class McpServerOauthLoginParams(
    val name: McpServerName,
    val scopes: List<String>? = null,
    val timeoutSecs: Long? = null,
    val threadId: ThreadId? = null,
    /** Omission lets the server automatically discover the registration strategy. */
    val clientRegistration: McpServerOauthClientRegistration? = null,
)

/**
 * OAuth client registration strategy: automatic discovery, client-id metadata document, or dynamic registration; unknown values are retained.
 */
@Serializable
@JvmInline
value class McpServerOauthClientRegistration(val value: String) {
    companion object {
        val Auto = McpServerOauthClientRegistration("auto")
        val Cimd = McpServerOauthClientRegistration("cimd")
        val Dcr = McpServerOauthClientRegistration("dcr")
    }
}

/** OAuth authorization URL returned for application-mediated login; CoKit does not open it or infer successful authentication. */
@Serializable
data class McpServerOauthLoginResult(
    val authorizationUrl: String,
)

/** Empty parameters requesting reload of app-server MCP configuration. */
@Serializable
data object McpConfigReloadParams

/**
 * Lists MCP discovery and authentication state, optionally within one thread. Null paging and detail options retain server defaults.
 */
@Serializable
data class McpServerStatusListParams(
    val cursor: CodexCursor? = null,
    val detail: McpServerStatusDetail? = null,
    val limit: Int? = null,
    val threadId: ThreadId? = null,
    /** Restricts discovery to one server; unknown names return an empty page. */
    val serverName: McpServerName? = null,
)

/** Presentation metadata only; applications decide whether and how to render it. */
@Serializable
data class McpAppUi(
    val resourceUri: McpResourceUri,
    val preferredModelDisplayMode: McpAppDisplayMode,
)

/** Server-provided MCP app presentation preference; unknown values remain readable and do not trigger rendering. */
@Serializable
@JvmInline
value class McpAppDisplayMode(val value: String) {
    companion object {
        val Inline = McpAppDisplayMode("inline")
        val Fullscreen = McpAppDisplayMode("fullscreen")
    }
}

/** One page of MCP server status; null [nextCursor] means no continuation was reported. */
@Serializable
data class McpServerStatusListResult(
    val data: List<McpServerStatus> = emptyList(),
    val nextCursor: CodexCursor? = null,
)

/**
 * MCP discovery, authentication, and runtime state. CoKit treats capabilities, schemas, metadata, and diagnostic text as untrusted protocol data.
 *
 * @property toolsError Optional tool-discovery failure; an empty tools map alone does not explain the failure.
 * @property runtimeStatus Optional connection state, separate from authentication state.
 */
@Serializable
data class McpServerStatus(
    val name: McpServerName,
    val authStatus: McpAuthStatus,
    val resources: List<McpResource> = emptyList(),
    val resourceTemplates: List<McpResourceTemplate> = emptyList(),
    val tools: Map<McpToolName, McpTool> = emptyMap(),
    val serverInfo: McpServerInfo? = null,
    val httpOrigin: String? = null,
    val pluginId: String? = null,
    val runtimeStatus: McpServerConnectionStatus? = null,
    val serverCapabilities: CodexJsonPayload? = null,
    val toolsError: String? = null,
)

/** Identity and presentation metadata reported by the MCP server, without any local trust verification. */
@Serializable
data class McpServerInfo(
    val name: String,
    val version: String,
    val description: String? = null,
    val icons: CodexJsonPayload? = null,
    val title: String? = null,
    val websiteUrl: String? = null,
)

/**
 * Discoverable MCP resource metadata; reading it requires a separate resource-read request.
 *
 * @property size Optional resource size in bytes.
 * @property meta Original MCP _meta payload preserved without interpretation.
 */
@Serializable
data class McpResource(
    val uri: McpResourceUri,
    val name: String,
    @SerialName("_meta")
    val meta: CodexJsonPayload? = null,
    val annotations: CodexJsonPayload? = null,
    val description: String? = null,
    val icons: CodexJsonPayload? = null,
    val mimeType: String? = null,
    val size: Long? = null,
    val title: String? = null,
)

/** Parameterized URI template advertised by an MCP server; applications supply parameters before requesting a resource. */
@Serializable
data class McpResourceTemplate(
    val uriTemplate: String,
    val name: String,
    val annotations: CodexJsonPayload? = null,
    val description: String? = null,
    val mimeType: String? = null,
    val title: String? = null,
)

/** MCP tool definition with opaque input and optional output schemas; listing a tool does not authorize invocation. */
@Serializable
data class McpTool(
    val name: String,
    val inputSchema: CodexJsonPayload,
    @SerialName("_meta")
    val meta: CodexJsonPayload? = null,
    val annotations: CodexJsonPayload? = null,
    val description: String? = null,
    val icons: CodexJsonPayload? = null,
    val outputSchema: CodexJsonPayload? = null,
    val title: String? = null,
)

/**
 * Requests an MCP resource through app-server in an optional thread or connector context.
 *
 * @property target Optional connector/link selection; when present, a null link id explicitly requests no-auth access subject to server policy.
 */
@Serializable
data class McpResourceReadParams(
    val server: McpServerName,
    val uri: McpResourceUri,
    val threadId: ThreadId? = null,
    val connectorId: String? = null,
    val originCallId: String? = null,
    val target: McpResourceReadTarget? = null,
)

/** MCP resource contents, which may contain text or base64 binary data and must be treated as untrusted input. */
@Serializable
data class McpResourceReadResult(
    val contents: List<McpResourceContent> = emptyList(),
)

/** One MCP resource content record, decoded according to the presence of text or blob fields. */
@Serializable(with = McpResourceContentSerializer::class)
sealed interface McpResourceContent {
    val uri: McpResourceUri
    val mimeType: String?
    val meta: CodexJsonPayload?

    /** Text resource contents provided by the MCP server, with optional MIME type and metadata. */
    @Serializable
    data class Text(
        override val uri: McpResourceUri,
        val text: String,
        override val mimeType: String? = null,
        @SerialName("_meta")
        override val meta: CodexJsonPayload? = null,
    ) : McpResourceContent

    /** Binary resource contents encoded as base64 by the MCP server; decoding belongs to the application. */
    @Serializable
    data class Blob(
        override val uri: McpResourceUri,
        val blob: String,
        override val mimeType: String? = null,
        @SerialName("_meta")
        override val meta: CodexJsonPayload? = null,
    ) : McpResourceContent
}

/** JSON content selector that prefers text when present, otherwise decodes blob; a record with neither field fails decoding. */
object McpResourceContentSerializer :
    JsonContentPolymorphicSerializer<McpResourceContent>(McpResourceContent::class) {
    override fun selectDeserializer(
        element: JsonElement,
    ): DeserializationStrategy<McpResourceContent> {
        val content = element.jsonObject
        return when {
            "text" in content -> McpResourceContent.Text.serializer()
            "blob" in content -> McpResourceContent.Blob.serializer()
            else -> throw SerializationException("Expected MCP resource content text or blob payload")
        }
    }
}

/** Explicit MCP tool invocation through app-server; applications own authorization and validation of the opaque arguments. */
@Serializable
data class McpServerToolCallParams(
    val server: McpServerName,
    val threadId: ThreadId,
    val tool: McpToolName,
    val arguments: CodexJsonPayload? = null,
    @SerialName("_meta")
    val meta: CodexJsonPayload? = null,
)

/**
 * MCP tool outcome preserved without interpreting its content.
 *
 * @property isError Optional tool-level error indicator, independent of JSON-RPC success.
 * @property structuredContent Optional structured result preserved as JSON.
 */
@Serializable
data class McpServerToolCallResult(
    val content: CodexJsonPayload,
    val isError: Boolean? = null,
    @SerialName("_meta")
    val meta: CodexJsonPayload? = null,
    val structuredContent: CodexJsonPayload? = null,
)

/** A null linkId explicitly requests no-auth access, subject to server policy. */
@Serializable
data class McpResourceReadTarget(val connectorId: String, val linkId: String?) {
    override fun toString(): String = "McpResourceReadTarget(connectorId=[redacted], hasLinkId=${linkId != null})"
}

/** App-server runtime connection state for an MCP server; unknown wire strings are retained. */
@Serializable
@JvmInline
value class McpServerConnectionStatus(val value: String) {
    companion object {
        val NotStarted = McpServerConnectionStatus("notStarted")
        val Starting = McpServerConnectionStatus("starting")
        val Connected = McpServerConnectionStatus("connected")
        val AuthenticationRequired = McpServerConnectionStatus("authenticationRequired")
        val Failed = McpServerConnectionStatus("failed")
        val Cancelled = McpServerConnectionStatus("cancelled")
        val Disabled = McpServerConnectionStatus("disabled")
    }
}
