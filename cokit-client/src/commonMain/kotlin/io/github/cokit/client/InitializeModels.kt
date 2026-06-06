package io.github.cokit.client

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
    val optOutNotificationMethods: List<String> = emptyList(),
    val requestAttestation: Boolean = false,
)

@Serializable
data class InitializeParams(
    val clientInfo: ClientInfo,
    val capabilities: InitializeCapabilities? = null,
)
