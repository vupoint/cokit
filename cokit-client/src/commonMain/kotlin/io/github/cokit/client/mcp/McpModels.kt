package io.github.cokit.client.mcp

import io.github.cokit.client.CodexCursor
import io.github.cokit.client.CodexJsonPayload
import io.github.cokit.client.ThreadId
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
@JvmInline
value class McpServerName(val value: String)

@Serializable
@JvmInline
value class McpResourceUri(val value: String)

@Serializable
@JvmInline
value class McpToolName(val value: String)

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

@Serializable
@JvmInline
value class McpServerStatusDetail(val value: String) {
    companion object {
        val Full = McpServerStatusDetail("full")
        val ToolsAndAuthOnly = McpServerStatusDetail("toolsAndAuthOnly")
    }
}

@Serializable
data class McpServerOauthLoginParams(
    val name: McpServerName,
    val scopes: List<String>? = null,
    val timeoutSecs: Long? = null,
)

@Serializable
data class McpServerOauthLoginResult(
    val authorizationUrl: String,
)

@Serializable
data object McpConfigReloadParams

@Serializable
data class McpServerStatusListParams(
    val cursor: CodexCursor? = null,
    val detail: McpServerStatusDetail? = null,
    val limit: Int? = null,
    val threadId: ThreadId? = null,
)

@Serializable
data class McpServerStatusListResult(
    val data: List<McpServerStatus> = emptyList(),
    val nextCursor: CodexCursor? = null,
)

@Serializable
data class McpServerStatus(
    val name: McpServerName,
    val authStatus: McpAuthStatus,
    val resources: List<McpResource> = emptyList(),
    val resourceTemplates: List<McpResourceTemplate> = emptyList(),
    val tools: Map<McpToolName, McpTool> = emptyMap(),
    val serverInfo: McpServerInfo? = null,
)

@Serializable
data class McpServerInfo(
    val name: String,
    val version: String,
    val description: String? = null,
    val icons: CodexJsonPayload? = null,
    val title: String? = null,
    val websiteUrl: String? = null,
)

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

@Serializable
data class McpResourceTemplate(
    val uriTemplate: String,
    val name: String,
    val annotations: CodexJsonPayload? = null,
    val description: String? = null,
    val mimeType: String? = null,
    val title: String? = null,
)

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

@Serializable
data class McpResourceReadParams(
    val server: McpServerName,
    val uri: McpResourceUri,
    val threadId: ThreadId? = null,
)

@Serializable
data class McpResourceReadResult(
    val contents: List<McpResourceContent> = emptyList(),
)

@Serializable(with = McpResourceContentSerializer::class)
sealed interface McpResourceContent {
    val uri: McpResourceUri
    val mimeType: String?
    val meta: CodexJsonPayload?

    @Serializable
    data class Text(
        override val uri: McpResourceUri,
        val text: String,
        override val mimeType: String? = null,
        @SerialName("_meta")
        override val meta: CodexJsonPayload? = null,
    ) : McpResourceContent

    @Serializable
    data class Blob(
        override val uri: McpResourceUri,
        val blob: String,
        override val mimeType: String? = null,
        @SerialName("_meta")
        override val meta: CodexJsonPayload? = null,
    ) : McpResourceContent
}

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

@Serializable
data class McpServerToolCallParams(
    val server: McpServerName,
    val threadId: ThreadId,
    val tool: McpToolName,
    val arguments: CodexJsonPayload? = null,
    @SerialName("_meta")
    val meta: CodexJsonPayload? = null,
)

@Serializable
data class McpServerToolCallResult(
    val content: CodexJsonPayload,
    val isError: Boolean? = null,
    @SerialName("_meta")
    val meta: CodexJsonPayload? = null,
    val structuredContent: CodexJsonPayload? = null,
)
