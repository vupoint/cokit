package io.github.vupoint.cokit.client

import kotlinx.serialization.Serializable

/**
 * Identifies the connecting application during initialization.
 *
 * @property name Application identifier supplied to app-server.
 * @property title Human-readable application title.
 * @property version Application version, independent of the server version.
 */
@Serializable
data class ClientInfo(
    val name: String,
    val title: String,
    val version: String,
)

/**
 * Capabilities advertised in the initialization handshake.
 *
 * @property experimentalApi Enables experimental protocol operations; Kotlin API opt-in is also required.
 * @property optOutNotificationMethods Exact notification method names the client asks the server to suppress.
 * @property requestAttestation Declares support for server-initiated attestation requests; it does not approve them.
 */
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

/**
 * Payload for `initialize`, sent before normal requests and the `initialized` notification.
 *
 * A null [capabilities] leaves capability selection to the server defaults.
 */
@Serializable
data class InitializeParams(
    val clientInfo: ClientInfo,
    val capabilities: InitializeCapabilities? = null,
)
