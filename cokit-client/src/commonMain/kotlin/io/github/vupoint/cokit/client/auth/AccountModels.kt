package io.github.vupoint.cokit.client.auth

import io.github.vupoint.cokit.client.ExperimentalCodexApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = AccountEmailSerializer::class)
class AccountEmail(
    val value: String,
) {
    override fun equals(other: Any?): Boolean =
        other is AccountEmail && value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "<redacted>"
}

object AccountEmailSerializer : KSerializer<AccountEmail> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("AccountEmail", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): AccountEmail =
        AccountEmail(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: AccountEmail) {
        encoder.encodeString(value.value)
    }
}

@Serializable
@JvmInline
value class AccountPlanType(val value: String) {
    companion object {
        val Free = AccountPlanType("free")
        val Go = AccountPlanType("go")
        val Plus = AccountPlanType("plus")
        val Pro = AccountPlanType("pro")
        val ProLite = AccountPlanType("prolite")
        val Team = AccountPlanType("team")
        val SelfServeBusinessUsageBased = AccountPlanType("self_serve_business_usage_based")
        val Business = AccountPlanType("business")
        val EnterpriseCbpUsageBased = AccountPlanType("enterprise_cbp_usage_based")
        val Enterprise = AccountPlanType("enterprise")
        val Edu = AccountPlanType("edu")
        val Unknown = AccountPlanType("unknown")
    }
}

@Serializable
data class AccountReadParams(
    val refreshToken: Boolean? = null,
)

@Serializable
data class AccountReadResult(
    val requiresOpenaiAuth: Boolean,
    val account: CodexAccount? = null,
)

@Serializable
sealed interface CodexAccount {
    @Serializable
    @SerialName("apiKey")
    data object ApiKey : CodexAccount

    @Serializable
    @SerialName("chatgpt")
    data class ChatGpt(
        val email: AccountEmail,
        val planType: AccountPlanType,
    ) : CodexAccount {
        override fun toString(): String =
            "ChatGpt(email=<redacted>, planType=$planType)"
    }

    @Serializable
    @SerialName("amazonBedrock")
    data object AmazonBedrock : CodexAccount
}

@Serializable
data object LogoutAccountParams

@Serializable
@JvmInline
value class LoginAccountId(val value: String)

@Serializable
sealed interface LoginAccountParams {
    @Serializable
    @SerialName("apiKey")
    data class ApiKey(
        val apiKey: String,
    ) : LoginAccountParams {
        override fun toString(): String =
            "ApiKey(apiKey=<redacted>)"
    }

    @Serializable
    @SerialName("chatgpt")
    data class ChatGpt(
        val codexStreamlinedLogin: Boolean? = null,
    ) : LoginAccountParams

    @Serializable
    @SerialName("chatgptDeviceCode")
    data object ChatGptDeviceCode : LoginAccountParams

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

@Serializable
sealed interface LoginAccountResult {
    @Serializable
    @SerialName("apiKey")
    data object ApiKey : LoginAccountResult

    @Serializable
    @SerialName("chatgpt")
    data class ChatGpt(
        val loginId: LoginAccountId,
        val authUrl: String,
    ) : LoginAccountResult {
        override fun toString(): String =
            "ChatGpt(loginId=$loginId, authUrl=<redacted>)"
    }

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

    @ExperimentalCodexApi
    @Serializable
    @SerialName("chatgptAuthTokens")
    data object ChatGptAuthTokens : LoginAccountResult
}

@Serializable
data class CancelLoginAccountParams(
    val loginId: LoginAccountId,
)

@Serializable
@JvmInline
value class CancelLoginAccountStatus(val value: String) {
    companion object {
        val Canceled = CancelLoginAccountStatus("canceled")
        val NotFound = CancelLoginAccountStatus("notFound")
    }
}

@Serializable
data class CancelLoginAccountResult(
    val status: CancelLoginAccountStatus,
)

@Serializable
data object AccountRateLimitsReadParams

@Serializable
data object AccountUsageReadParams

@Serializable
data class AccountRateLimitsResult(
    val rateLimits: AccountRateLimitSnapshot,
    val rateLimitsByLimitId: Map<String, AccountRateLimitSnapshot>? = null,
)

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

@Serializable
data class AccountRateLimitWindow(
    val usedPercent: Int,
    val resetsAt: Long? = null,
    val windowDurationMins: Long? = null,
)

@Serializable
data class AccountRateLimitStatus(
    val hasCredits: Boolean,
    val unlimited: Boolean,
    val balance: String? = null,
)

@Serializable
data class AccountSpendControlLimitSnapshot(
    val limit: String,
    val used: String,
    val remainingPercent: Int,
    val resetsAt: Long,
)

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

@Serializable
data class AccountUsageResult(
    val summary: AccountTokenUsageSummary,
    val dailyUsageBuckets: List<AccountTokenUsageDailyBucket>? = null,
)

@Serializable
data class AccountTokenUsageSummary(
    val lifetimeTokens: Long? = null,
    val peakDailyTokens: Long? = null,
    val currentStreakDays: Long? = null,
    val longestStreakDays: Long? = null,
    val longestRunningTurnSec: Long? = null,
)

@Serializable
data class AccountTokenUsageDailyBucket(
    val startDate: String,
    val tokens: Long,
)

@Serializable
data class SendAddCreditsNudgeEmailParams(
    val creditType: AddCreditsNudgeCreditType,
)

@Serializable
@JvmInline
value class AddCreditsNudgeCreditType(val value: String) {
    companion object {
        val Credits = AddCreditsNudgeCreditType("credits")
        val UsageLimit = AddCreditsNudgeCreditType("usage_limit")
    }
}

@Serializable
data class SendAddCreditsNudgeEmailResult(
    val status: AddCreditsNudgeEmailStatus,
)

@Serializable
@JvmInline
value class AddCreditsNudgeEmailStatus(val value: String) {
    companion object {
        val Sent = AddCreditsNudgeEmailStatus("sent")
        val CooldownActive = AddCreditsNudgeEmailStatus("cooldown_active")
    }
}
