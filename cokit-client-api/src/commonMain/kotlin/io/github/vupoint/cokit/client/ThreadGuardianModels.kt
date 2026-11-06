package io.github.vupoint.cokit.client

import kotlinx.serialization.Serializable

/**
 * Reviews an idle thread's final injected exec_command without executing it.
 * This subscribes the connection to the thread until explicit unsubscribe or disconnection.
 */
@ExperimentalCodexApi
@Serializable
data class ThreadGuardianReviewParams(val threadId: ThreadId)

/** Recorded review outcome; a null assessment means exec policy skipped approval. */
@ExperimentalCodexApi
@Serializable
data class ThreadGuardianReviewResult(
    val callId: String,
    val assessment: ThreadGuardianAssessment? = null,
)

/** Server assessment for inspection; this does not authorize CoKit to execute a command. */
@ExperimentalCodexApi
@Serializable
data class ThreadGuardianAssessment(
    val outcome: GuardianAssessmentOutcome,
    val riskLevel: GuardianRiskLevel,
    val userAuthorization: GuardianUserAuthorization,
    val rationale: String,
) {
    override fun toString(): String = "ThreadGuardianAssessment(outcome=$outcome, riskLevel=$riskLevel, userAuthorization=$userAuthorization)"
}

/** Guardian result; unknown outcomes must not be interpreted as permission to execute. */
@ExperimentalCodexApi
@Serializable
@JvmInline
value class GuardianAssessmentOutcome(val value: String) {
    companion object {
        val Allow = GuardianAssessmentOutcome("allow")
        val Deny = GuardianAssessmentOutcome("deny")
    }
}

/** Unstable server risk category; future strings remain readable. */
@ExperimentalCodexApi
@Serializable
@JvmInline
value class GuardianRiskLevel(val value: String) {
    companion object {
        val Low = GuardianRiskLevel("low")
        val Medium = GuardianRiskLevel("medium")
        val High = GuardianRiskLevel("high")
        val Critical = GuardianRiskLevel("critical")
    }
}

/** Unstable server assessment of authorization; this is not an application approval decision. */
@ExperimentalCodexApi
@Serializable
@JvmInline
value class GuardianUserAuthorization(val value: String) {
    companion object {
        val Unknown = GuardianUserAuthorization("unknown")
        val Low = GuardianUserAuthorization("low")
        val Medium = GuardianUserAuthorization("medium")
        val High = GuardianUserAuthorization("high")
    }
}
