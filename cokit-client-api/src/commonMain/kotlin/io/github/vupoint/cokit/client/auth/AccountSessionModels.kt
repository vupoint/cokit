@file:OptIn(io.github.vupoint.cokit.client.ExperimentalCodexApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)

package io.github.vupoint.cokit.client.auth

import io.github.vupoint.cokit.client.CodexJsonPayload
import io.github.vupoint.cokit.client.ExperimentalCodexApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonObject

/** Opaque active-login identity, distinct from a browser login attempt; diagnostics redact it. */
@ExperimentalCodexApi
@Serializable
@JvmInline
value class AccountSessionId(val value: String) {
    override fun toString(): String = "AccountSessionId(<redacted>)"
}

/** Workspace account identity, distinct from a user identity; diagnostics redact it. */
@ExperimentalCodexApi
@Serializable
@JvmInline
value class AccountWorkspaceId(val value: String) {
    override fun toString(): String = "AccountWorkspaceId(<redacted>)"
}

/** Reads sessions; refreshing metadata explicitly fetches workspace memberships. */
@ExperimentalCodexApi
@Serializable
data class AccountSessionListParams(val refreshWorkspaceMetadata: Boolean? = null)

/** Sessions visible to this app-server, including the currently selected login when available. */
@ExperimentalCodexApi
@Serializable
data class AccountSessionListResult(
    val sessions: List<AccountSession>,
    val activeSessionId: AccountSessionId? = null,
) {
    override fun toString(): String = "AccountSessionListResult(sessionCount=${sessions.size})"
}

/** Private login and workspace metadata; reading it neither authenticates nor switches workspaces. */
@ExperimentalCodexApi
@Serializable
data class AccountSession(
    val sessionId: AccountSessionId,
    val isActive: Boolean,
    val lastUsedAt: Long,
    val workspaces: List<AccountSessionWorkspace>,
    val email: AccountEmail? = null,
    val userId: String? = null,
    val displayName: String? = null,
    val imageUrl: String? = null,
    val selectedWorkspaceAccountId: AccountWorkspaceId? = null,
) {
    override fun toString(): String = "AccountSession(<redacted>)"
}

/** A workspace available within a login; all identifying metadata is private. */
@ExperimentalCodexApi
@Serializable
data class AccountSessionWorkspace(
    val accountId: AccountWorkspaceId,
    val name: String? = null,
    val imageUrl: String? = null,
    val kind: AccountSessionWorkspaceKind? = null,
) {
    override fun toString(): String = "AccountSessionWorkspace(<redacted>)"
}

/** Workspace category; unknown server strings remain readable. */
@ExperimentalCodexApi
@Serializable
@JvmInline
value class AccountSessionWorkspaceKind(val value: String) {
    companion object {
        val Personal = AccountSessionWorkspaceKind("personal")
        val Workspace = AccountSessionWorkspaceKind("workspace")
    }
}

/**
 * Explicit workspace switch. App-server may stop loaded threads and replace credentials.
 * Browser sign-in is permitted only when requested; CoKit never opens the returned URL or retries.
 */
@ExperimentalCodexApi
@Serializable
data class AccountSessionSwitchParams(
    val sessionId: AccountSessionId,
    val accountId: AccountWorkspaceId,
    val allowBrowserLogin: Boolean? = null,
) {
    override fun toString(): String = "AccountSessionSwitchParams(<redacted>)"
}

/** Untagged switch outcome; a browser login must complete before the workspace has switched. */
@ExperimentalCodexApi
@Serializable(with = AccountSessionSwitchResultSerializer::class)
sealed interface AccountSessionSwitchResult {
    /** Updated session snapshot after the server completed the switch. */
    data class Switched(val snapshot: AccountSessionListResult) : AccountSessionSwitchResult

    /** Application-owned browser flow; await account/login/completed or explicitly cancel [loginId]. */
    @Serializable
    data class BrowserLogin(
        @JsonNames("login_id") val loginId: LoginAccountId,
        @JsonNames("auth_url") val authUrl: String,
    ) : AccountSessionSwitchResult {
        override fun toString(): String = "BrowserLogin(<redacted>)"
    }

    /** Future result retained without interpreting it as a successful switch. */
    data class Unknown(val payload: CodexJsonPayload) : AccountSessionSwitchResult {
        override fun toString(): String = "Unknown(<redacted>)"
    }
}

internal object AccountSessionSwitchResultSerializer : KSerializer<AccountSessionSwitchResult> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): AccountSessionSwitchResult {
        val input = decoder as? JsonDecoder ?: throw SerializationException("Account session switching requires JSON")
        val payload = input.decodeJsonElement() as? JsonObject
            ?: throw SerializationException("Account session switch result must be an object")
        return when {
            "sessions" in payload -> AccountSessionSwitchResult.Switched(
                input.json.decodeFromJsonElement(AccountSessionListResult.serializer(), payload),
            )
            payload.keys.any { it in setOf("loginId", "authUrl", "login_id", "auth_url") } ->
                input.json.decodeFromJsonElement(AccountSessionSwitchResult.BrowserLogin.serializer(), payload)
            else -> AccountSessionSwitchResult.Unknown(CodexJsonPayload(payload))
        }
    }

    override fun serialize(encoder: Encoder, value: AccountSessionSwitchResult) {
        val output = encoder as? JsonEncoder ?: throw SerializationException("Account session switching requires JSON")
        val payload = when (value) {
            is AccountSessionSwitchResult.Switched -> output.json.encodeToJsonElement(AccountSessionListResult.serializer(), value.snapshot)
            is AccountSessionSwitchResult.BrowserLogin -> output.json.encodeToJsonElement(AccountSessionSwitchResult.BrowserLogin.serializer(), value)
            is AccountSessionSwitchResult.Unknown -> value.payload.element
        }
        output.encodeJsonElement(payload)
    }
}
