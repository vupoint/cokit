package io.github.vupoint.cokit.client.auth

import io.github.vupoint.cokit.client.CodexHostPath
import io.github.vupoint.cokit.client.ExperimentalCodexApi
import kotlinx.serialization.Serializable

/**
 * Configures Vertex AI after app-server verifies credentials. Omitted location selects global.
 * Omitting [credentialFile] clears the active user override and selects Application Default
 * Credentials. App-server rejects conflicting inherited settings without saving changes.
 * Paths refer to its host; CoKit neither reads credentials nor runs authentication commands.
 */
@ExperimentalCodexApi
@Serializable
data class GcpSetupParams(
    val projectId: String,
    val location: String? = null,
    val credentialFile: CodexHostPath? = null,
) {
    override fun toString(): String = "GcpSetupParams(<redacted>)"
}
