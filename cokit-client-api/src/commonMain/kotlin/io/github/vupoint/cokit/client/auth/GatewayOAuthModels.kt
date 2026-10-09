package io.github.vupoint.cokit.client.auth

import kotlinx.serialization.Serializable

/** Gateway credential readiness state; success does not prove a subsequent gateway request will succeed. */
@Serializable
@JvmInline
value class GatewayOAuthStatus(val value: String) {
    companion object {
        val NotReady = GatewayOAuthStatus("notReady")
        val Started = GatewayOAuthStatus("started")
        val Succeeded = GatewayOAuthStatus("succeeded")
        val Failed = GatewayOAuthStatus("failed")
    }
}

/** Read-only readiness probe; succeeded means locally usable credentials, not a verified gateway request. */
@Serializable
data class GatewayOAuthReadResult(
    val providerId: String,
    val providerName: String,
    val required: Boolean,
    val status: GatewayOAuthStatus? = null,
    val error: String? = null,
) {
    override fun toString(): String = "GatewayOAuthReadResult(required=$required, status=$status, hasError=${error != null})"
}
