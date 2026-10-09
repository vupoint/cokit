package io.github.vupoint.cokit.client.auth

import io.github.vupoint.cokit.client.CodexTimestamp
import io.github.vupoint.cokit.client.ExperimentalCodexApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Private account email whose string representation is redacted; accessing [value] or serializing it reveals the original address.
 */
@Serializable(with = AccountEmailSerializer::class)
class AccountEmail(
    val value: String,
) {
    override fun equals(other: Any?): Boolean =
        other is AccountEmail && value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "<redacted>"
}

/**
 * Encodes and decodes the original account email as a wire string; redaction applies only to the model's string representation.
 */
object AccountEmailSerializer : KSerializer<AccountEmail> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("AccountEmail", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): AccountEmail =
        AccountEmail(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: AccountEmail) {
        encoder.encodeString(value.value)
    }
}

/** Server-reported subscription plan; unknown plan strings remain readable for forward compatibility. */
@Serializable
@JvmInline
value class AccountPlanType(val value: String) {
    companion object {
        val Free = AccountPlanType("free")
        val Go = AccountPlanType("go")
        val Plus = AccountPlanType("plus")
        val Pro = AccountPlanType("pro")
        val ProLite = AccountPlanType("prolite")
        val ProMax = AccountPlanType("promax")
        val Team = AccountPlanType("team")
        val SelfServeBusinessUsageBased = AccountPlanType("self_serve_business_usage_based")
        val Business = AccountPlanType("business")
        val EnterpriseCbpUsageBased = AccountPlanType("enterprise_cbp_usage_based")
        val Enterprise = AccountPlanType("enterprise")
        val Edu = AccountPlanType("edu")
        val Unknown = AccountPlanType("unknown")
    }
}

/**
 * Options for reading the current app-server authentication state.
 *
 * @property refreshToken Optional request to refresh authentication tokens; null leaves the server default in effect.
 */
@Serializable
data class AccountReadParams(
    val refreshToken: Boolean? = null,
)

/**
 * Current account state without credential material.
 *
 * @property requiresOpenaiAuth Whether the server requires OpenAI authentication.
 * @property account Null when the server reports no authenticated account.
 */
@Serializable
data class AccountReadResult(
    val requiresOpenaiAuth: Boolean,
    val account: CodexAccount? = null,
)

/** Authentication mode reported by app-server; ChatGPT accounts expose private email and plan metadata but no access token. */
@Serializable
sealed interface CodexAccount {
    /**
     * Account authenticated by API key; the key is not exposed in this read result.
     */
    @Serializable
    @SerialName("apiKey")
    data object ApiKey : CodexAccount

    /**
     * ChatGPT account identity and subscription plan. The email is private even though string rendering redacts it.
     */
    @Serializable
    @SerialName("chatgpt")
    data class ChatGpt(
        val email: AccountEmail,
        val planType: AccountPlanType,
    ) : CodexAccount {
        override fun toString(): String =
            "ChatGpt(email=<redacted>, planType=$planType)"
    }

    /**
     * Account using the Amazon Bedrock authentication mode reported by app-server.
     */
    @Serializable
    @SerialName("amazonBedrock")
    data object AmazonBedrock : CodexAccount
}

/** Empty parameters for explicitly logging the app-server account out. */
@Serializable
data object LogoutAccountParams

/** Identifier of a pending login flow, used by cancellation and login-completion notifications. */
@Serializable
@JvmInline
value class LoginAccountId(val value: String)

/**
 * Selects an explicit account login flow. Applications own consent, browser interaction, and secure handling of raw credentials.
 */
@Serializable
sealed interface LoginAccountParams {
    /**
     * Submits an API key to app-server. String rendering redacts the key; the raw property and wire payload remain sensitive.
     */
    @Serializable
    @SerialName("apiKey")
    data class ApiKey(
        val apiKey: String,
    ) : LoginAccountParams {
        override fun toString(): String =
            "ApiKey(apiKey=<redacted>)"
    }

    /**
     * Starts browser-mediated ChatGPT login; optional streamlined-login behavior is delegated to app-server.
     */
    @Serializable
    @SerialName("chatgpt")
    data class ChatGpt(
        val codexStreamlinedLogin: Boolean? = null,
    ) : LoginAccountParams

    /**
     * Starts ChatGPT device-code login, requiring later user verification.
     */
    @Serializable
    @SerialName("chatgptDeviceCode")
    data object ChatGptDeviceCode : LoginAccountParams

    /**
     * Experimental external-token login. The application owns token acquisition, storage, and consent; raw token and account-id fields are sensitive.
     */
    @ExperimentalCodexApi
    @Serializable
    @SerialName("chatgptAuthTokens")
    data class ChatGptAuthTokens(
        val accessToken: String,
        val chatgptAccountId: String,
        val chatgptPlanType: AccountPlanType? = null,
    ) : LoginAccountParams {
        override fun toString(): String =
            "ChatGptAuthTokens(accessToken=<redacted>, chatgptAccountId=<redacted>, chatgptPlanType=$chatgptPlanType)"
    }
}

/**
 * Login flow outcome or continuation data. Browser and device-code results require user interaction and later completion confirmation.
 */
@Serializable
sealed interface LoginAccountResult {
    /**
     * Acknowledges the API-key login flow without returning the key.
     */
    @Serializable
    @SerialName("apiKey")
    data object ApiKey : LoginAccountResult

    /**
     * Pending browser login with a flow id and sensitive authorization URL; the application must await login completion.
     */
    @Serializable
    @SerialName("chatgpt")
    data class ChatGpt(
        val loginId: LoginAccountId,
        val authUrl: String,
    ) : LoginAccountResult {
        override fun toString(): String =
            "ChatGpt(loginId=$loginId, authUrl=<redacted>)"
    }

    /**
     * Pending device verification with a flow id, verification URL, and sensitive user code.
     */
    @Serializable
    @SerialName("chatgptDeviceCode")
    data class ChatGptDeviceCode(
        val loginId: LoginAccountId,
        val verificationUrl: String,
        val userCode: String,
    ) : LoginAccountResult {
        override fun toString(): String =
            "ChatGptDeviceCode(loginId=$loginId, verificationUrl=<redacted>, userCode=<redacted>)"
    }

    /**
     * Acknowledges the experimental external-token login flow without returning the tokens.
     */
    @ExperimentalCodexApi
    @Serializable
    @SerialName("chatgptAuthTokens")
    data object ChatGptAuthTokens : LoginAccountResult
}

/** Cancels the pending login identified by [loginId]. */
@Serializable
data class CancelLoginAccountParams(
    val loginId: LoginAccountId,
)

/** Whether a pending login was canceled or no matching flow existed; unknown strings are retained. */
@Serializable
@JvmInline
value class CancelLoginAccountStatus(val value: String) {
    companion object {
        val Canceled = CancelLoginAccountStatus("canceled")
        val NotFound = CancelLoginAccountStatus("notFound")
    }
}

/** Server outcome of a login cancellation, which may report that the flow no longer exists. */
@Serializable
data class CancelLoginAccountResult(
    val status: CancelLoginAccountStatus,
)

/** Empty parameters for reading account usage limits without consuming reset credits. */
@Serializable
data object AccountRateLimitsReadParams

/** Empty parameters for reading account token-usage history. */
@Serializable
data object AccountUsageReadParams

/** Empty parameters for reading server-provided workspace announcements. */
@Serializable
data object AccountWorkspaceMessagesReadParams

/** Workspace announcements and whether the server has enabled the message feature. */
@Serializable
data class AccountWorkspaceMessagesResult(
    val featureEnabled: Boolean,
    val messages: List<WorkspaceMessage>,
)

/** Server-provided workspace announcement with optional creation and archival timestamps. */
@Serializable
data class WorkspaceMessage(
    val messageId: String,
    val messageType: WorkspaceMessageType,
    val messageBody: String,
    val createdAt: CodexTimestamp? = null,
    val archivedAt: CodexTimestamp? = null,
)

/** Announcement presentation category; unknown wire strings are retained. */
@Serializable
@JvmInline
value class WorkspaceMessageType(val value: String) {
    companion object {
        val Headline = WorkspaceMessageType("headline")
        val Announcement = WorkspaceMessageType("announcement")
        val Unknown = WorkspaceMessageType("unknown")
    }
}

/**
 * Explicit rate-limit reset attempt that may consume an account credit.
 *
 * @property idempotencyKey Reuse this key when retrying the same logical redemption; do not generate a new key for each retry.
 * @property creditId Optional specific credit selector; null delegates selection to app-server.
 */
@Serializable
data class ConsumeAccountRateLimitResetCreditParams(
    val idempotencyKey: String,
    val creditId: String? = null,
)

/** Outcome of a reset attempt; only the reset outcome confirms a rate-limit reset. */
@Serializable
data class ConsumeAccountRateLimitResetCreditResult(
    val outcome: ConsumeAccountRateLimitResetCreditOutcome,
)

/** Reset-credit redemption outcome, including no available credit or an already redeemed attempt; unknown strings are retained. */
@Serializable
@JvmInline
value class ConsumeAccountRateLimitResetCreditOutcome(val value: String) {
    companion object {
        val Reset = ConsumeAccountRateLimitResetCreditOutcome("reset")
        val NothingToReset = ConsumeAccountRateLimitResetCreditOutcome("nothingToReset")
        val NoCredit = ConsumeAccountRateLimitResetCreditOutcome("noCredit")
        val AlreadyRedeemed = ConsumeAccountRateLimitResetCreditOutcome("alreadyRedeemed")
    }
}

/**
 * Account usage-limit snapshots.
 *
 * @property rateLimits Legacy aggregate snapshot.
 * @property rateLimitsByLimitId Optional per-limit snapshots; absence does not imply unlimited usage.
 */
@Serializable
data class AccountRateLimitsResult(
    val rateLimits: AccountRateLimitSnapshot,
    val rateLimitsByLimitId: Map<String, AccountRateLimitSnapshot>? = null,
)

/**
 * Usage windows, credits, plan metadata, and optional spend-control state for a server-reported limit. Null fields mean unavailable data.
 */
@Serializable
data class AccountRateLimitSnapshot(
    val primary: AccountRateLimitWindow? = null,
    val secondary: AccountRateLimitWindow? = null,
    val credits: AccountRateLimitStatus? = null,
    val planType: AccountPlanType? = null,
    val limitId: String? = null,
    val limitName: String? = null,
    val individualLimit: AccountSpendControlLimitSnapshot? = null,
    val rateLimitReachedType: AccountRateLimitReachedType? = null,
)

/**
 * Consumed percentage of an account usage window.
 *
 * @property usedPercent Percentage consumed, rather than percentage remaining.
 * @property resetsAt Optional reset time in Unix epoch seconds.
 * @property windowDurationMins Optional duration of the window in minutes.
 */
@Serializable
data class AccountRateLimitWindow(
    val usedPercent: Int,
    val resetsAt: Long? = null,
    val windowDurationMins: Long? = null,
)

/** Credit availability reported by app-server; [balance] is kept as a string to preserve its server representation. */
@Serializable
data class AccountRateLimitStatus(
    val hasCredits: Boolean,
    val unlimited: Boolean,
    val balance: String? = null,
)

/**
 * Server-reported account spend-control amounts, preserved as strings rather than converted to floating point.
 *
 * @property remainingPercent Percentage remaining under this limit.
 * @property resetsAt Reset time in Unix epoch seconds.
 */
@Serializable
data class AccountSpendControlLimitSnapshot(
    val limit: String,
    val used: String,
    val remainingPercent: Int,
    val resetsAt: Long,
)

/** Server explanation of an exhausted rate or workspace usage limit; unknown strings are retained. */
@Serializable
@JvmInline
value class AccountRateLimitReachedType(val value: String) {
    companion object {
        val RateLimitReached = AccountRateLimitReachedType("rate_limit_reached")
        val WorkspaceOwnerCreditsDepleted = AccountRateLimitReachedType("workspace_owner_credits_depleted")
        val WorkspaceMemberCreditsDepleted = AccountRateLimitReachedType("workspace_member_credits_depleted")
        val WorkspaceOwnerUsageLimitReached = AccountRateLimitReachedType("workspace_owner_usage_limit_reached")
        val WorkspaceMemberUsageLimitReached = AccountRateLimitReachedType("workspace_member_usage_limit_reached")
    }
}

/** Account token-usage summary with optional daily history; missing buckets are unavailable rather than zero usage. */
@Serializable
data class AccountUsageResult(
    val summary: AccountTokenUsageSummary,
    val dailyUsageBuckets: List<AccountTokenUsageDailyBucket>? = null,
)

/**
 * Server-computed lifetime and activity statistics; null fields indicate unavailable statistics.
 *
 * @property longestRunningTurnSec Duration of the longest running turn in seconds when reported.
 */
@Serializable
data class AccountTokenUsageSummary(
    val lifetimeTokens: Long? = null,
    val peakDailyTokens: Long? = null,
    val currentStreakDays: Long? = null,
    val longestStreakDays: Long? = null,
    val longestRunningTurnSec: Long? = null,
)

/** Token count for a server-reported date bucket; [startDate] retains the original date string. */
@Serializable
data class AccountTokenUsageDailyBucket(
    val startDate: String,
    val tokens: Long,
)

/** Explicit request to send a credit or usage-limit nudge email; reading account limits does not send it. */
@Serializable
data class SendAddCreditsNudgeEmailParams(
    val creditType: AddCreditsNudgeCreditType,
)

/** Credit or usage-limit category selected for an account nudge email; unknown strings are retained. */
@Serializable
@JvmInline
value class AddCreditsNudgeCreditType(val value: String) {
    companion object {
        val Credits = AddCreditsNudgeCreditType("credits")
        val UsageLimit = AddCreditsNudgeCreditType("usage_limit")
    }
}

/** Server result of a nudge email request, including suppression by an active cooldown. */
@Serializable
data class SendAddCreditsNudgeEmailResult(
    val status: AddCreditsNudgeEmailStatus,
)

/** Whether the nudge was sent or withheld by cooldown; unknown wire strings are retained. */
@Serializable
@JvmInline
value class AddCreditsNudgeEmailStatus(val value: String) {
    companion object {
        val Sent = AddCreditsNudgeEmailStatus("sent")
        val CooldownActive = AddCreditsNudgeEmailStatus("cooldown_active")
    }
}
