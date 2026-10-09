@file:OptIn(ExperimentalCodexApi::class)

package io.github.vupoint.cokit.client

import kotlin.io.encoding.Base64
import kotlinx.serialization.Serializable

/** Local authenticator availability; this does not initiate biometric verification. */
@ExperimentalCodexApi
@Serializable
data class UserVerificationStatusResult(
    val credentialId: String? = null,
    val unavailableMessage: String? = null,
    val unavailableReason: UserVerificationUnavailableReason? = null,
) {
    override fun toString(): String = "UserVerificationStatusResult(unavailableReason=$unavailableReason)"
}

/** Experimental reason local user verification is unavailable; arbitrary strings preserve new reasons. */
@ExperimentalCodexApi
@Serializable
@JvmInline
value class UserVerificationUnavailableReason(val value: String) {
    companion object {
        val CredentialMissing = UserVerificationUnavailableReason("credentialMissing")
        val BiometricsUnavailable = UserVerificationUnavailableReason("biometricsUnavailable")
        val ProviderUnavailable = UserVerificationUnavailableReason("providerUnavailable")
    }
}

/**
 * Experimental authenticator enrollment output, including any reported public key.
 *
 * Its string representation redacts credential and key fields.
 */
@ExperimentalCodexApi
@Serializable
data class UserVerificationEnrollResult(
    val credentialId: String,
    val algorithm: String? = null,
    val publicKey: String? = null,
) {
    override fun toString(): String = "UserVerificationEnrollResult([redacted])"
}

/**
 * Signs only caller-approved display context and a backend-supplied challenge.
 *
 * @property challenge Canonical unpadded base64url encoding of 1 to 4096 challenge bytes.
 * @property title Display title containing 1 to 256 UTF-8 bytes.
 * @property description Display description containing at most 4096 UTF-8 bytes.
 */
@ExperimentalCodexApi
@Serializable
data class UserVerificationVerifyParams(val challenge: String, val title: String, val description: String) {
    init {
        require(challenge.length in 2..5462 && challenge.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '-' || it == '_' }) {
            "Challenge must be unpadded base64url for 1 to 4096 bytes"
        }
        val encoding = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
        val bytes = encoding.decode(challenge)
        require(bytes.size in 1..4096 && encoding.encode(bytes) == challenge) { "Challenge must be canonical unpadded base64url" }
        require(title.encodeToByteArray().size in 1..256) { "Title must contain 1 to 256 UTF-8 bytes" }
        require(description.encodeToByteArray().size <= 4096) { "Description must contain at most 4096 UTF-8 bytes" }
    }

    override fun toString(): String = "UserVerificationVerifyParams([redacted])"
}

/**
 * Experimental signature proof to forward to the challenge issuer for validation.
 *
 * Its string representation redacts the credential ID and signature.
 */
@ExperimentalCodexApi
@Serializable
data class UserVerificationProof(val credentialId: String, val signature: String) {
    override fun toString(): String = "UserVerificationProof([redacted])"
}

/** Experimental verification output containing the generated signature proof. */
@ExperimentalCodexApi
@Serializable
data class UserVerificationVerifyResult(val proof: UserVerificationProof)

/** A cancellation acknowledgement does not confirm that the native prompt has closed. */
@ExperimentalCodexApi
@Serializable
data class UserVerificationCancelParams(val requestId: CodexRequestId)
