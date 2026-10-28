package io.github.vupoint.cokit.client

import kotlinx.serialization.Serializable

/**
 * Generates one response on an ephemeral fork of a loaded thread's latest terminal turn.
 * App-server rejects stale or active sources. Reasoning effort applies only on supported models.
 * Capture the RPC request ID to cancel explicitly; coroutine cancellation alone does not stop it.
 */
@ExperimentalCodexApi
@Serializable
data class ThreadSampleParams(
    val threadId: ThreadId,
    val expectedTurnId: TurnId,
    val prompt: String,
    val effort: ReasoningEffort? = null,
    val threadSource: ThreadSource? = null,
    val turnTrigger: String? = null,
    val responsesapiClientMetadata: Map<String, String>? = null,
    val disableMcp: Boolean? = null,
) {
    override fun toString(): String = "ThreadSampleParams(<redacted>)"
}

/** Single sampled response; null text can mean a tool call or a completion without text. */
@ExperimentalCodexApi
@Serializable
data class ThreadSampleResult(
    val sourceTurnId: TurnId,
    val responseId: String,
    val text: String? = null,
    val inputTokens: Long? = null,
    val cachedInputTokens: Long? = null,
    val outputTokens: Long? = null,
) {
    override fun toString(): String = "ThreadSampleResult(<redacted>)"
}

/** Cancels a sample by its exact numeric or string request identity on this connection. */
@ExperimentalCodexApi
@Serializable
data class ThreadSampleCancelParams(val requestId: CodexRequestId)

/** Reads memory using the selected pipeline of an already started or resumed thread. */
@ExperimentalCodexApi
@Serializable
data class ThreadMemoryReadParams(val threadId: ThreadId)

/** Null summary means disabled, missing, unreadable, or blank memory; it is not a readiness signal. */
@ExperimentalCodexApi
@Serializable
data class ThreadMemoryReadResult(val summary: String? = null) {
    override fun toString(): String = "ThreadMemoryReadResult(<redacted>)"
}
