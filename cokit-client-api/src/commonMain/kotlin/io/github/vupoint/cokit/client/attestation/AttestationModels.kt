package io.github.vupoint.cokit.client.attestation

import kotlinx.serialization.Serializable

@Serializable
data object AttestationGenerateRequest

@Serializable
data class AttestationGenerateResponse(
    val token: String,
)

fun interface AttestationGenerateHandler {
    suspend fun generate(request: AttestationGenerateRequest): AttestationGenerateResponse
}
