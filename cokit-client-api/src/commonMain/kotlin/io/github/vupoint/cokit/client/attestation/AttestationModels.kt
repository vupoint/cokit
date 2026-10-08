package io.github.vupoint.cokit.client.attestation

import kotlinx.serialization.Serializable

/** Empty server-initiated request asking the application to generate an attestation. */
@Serializable
data object AttestationGenerateRequest

/**
 * Application-generated attestation returned to app-server.
 *
 * @property token Attestation material; applications must avoid exposing it in logs or telemetry.
 */
@Serializable
data class AttestationGenerateResponse(
    val token: String,
)

/** Explicit application attestation provider. Without a handler, CoKit responds with unsupported status and generates no token. */
fun interface AttestationGenerateHandler {
    /** Produces attestation material only after the application authorizes the server request. */
    suspend fun generate(request: AttestationGenerateRequest): AttestationGenerateResponse
}
