package io.github.vupoint.cokit.client

import kotlinx.serialization.Serializable

@Serializable
data class ClientInfo(
    val name: String,
    val title: String,
    val version: String,
)

@Serializable
data class InitializeCapabilities(
    val experimentalApi: Boolean = false,
    val mcpServerOpenaiFormElicitation: Boolean? = null,
    val optOutNotificationMethods: List<String> = emptyList(),
    val requestAttestation: Boolean = false,
    /** MCP extension settings, for example an `openai/form` declaration. */
    val extensions: Map<String, CodexJsonPayload>? = null,
    /** Opts this server runtime into explicit gateway OAuth; later connections cannot undo it. */
    val explicitGatewayOauth: Boolean? = null,
)

@Serializable
data class InitializeParams(
    val clientInfo: ClientInfo,
    val capabilities: InitializeCapabilities? = null,
)
