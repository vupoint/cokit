package io.github.cokit.client.auth

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
