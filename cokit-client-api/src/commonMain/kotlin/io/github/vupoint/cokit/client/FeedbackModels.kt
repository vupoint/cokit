package io.github.vupoint.cokit.client

import kotlinx.serialization.Serializable

/**
 * Saves a diagnostic ZIP on the app-server host without uploading it.
 * [savePath] must be absolute and unused. The caller owns the file even after disconnection.
 * Logs and extra files may contain private data; collecting them requires an explicit call.
 */
@Serializable
data class FeedbackSaveParams(
    val classification: String,
    val savePath: CodexHostPath,
    val reason: String? = null,
    val threadId: ThreadId? = null,
    val includeLogs: Boolean? = null,
    val extraLogFiles: List<CodexHostPath>? = null,
    val tags: Map<String, String>? = null,
) {
    override fun toString(): String = "FeedbackSaveParams(<redacted>)"
}

/** Saved host ZIP and reported thread identity; the optional hash describes the base instructions. */
@Serializable
data class FeedbackSaveResult(
    val savedPath: CodexHostPath,
    val threadId: ThreadId,
    val promptHash: String? = null,
) {
    override fun toString(): String = "FeedbackSaveResult(<redacted>)"
}
