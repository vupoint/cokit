package io.github.vupoint.cokit.client

import kotlinx.serialization.Serializable

/** Persists an attached fork hidden from ordinary lists; incompatible with ephemeral forks. */
@Serializable
@JvmInline
value class ThreadAttachmentMode @ExperimentalCodexApi constructor(val enabled: Boolean)

/** Disables inherited incremental MCP tools on Responses Lite; requires prediction mode. */
@Serializable
@JvmInline
value class ThreadPredictionMcpDisabled @ExperimentalCodexApi constructor(val enabled: Boolean)

/** Lists only persistent forks attached to this source; Kotlin opt-in is required at construction. */
@Serializable
@JvmInline
value class ThreadAttachedTo @ExperimentalCodexApi constructor(val threadId: ThreadId)

/** Merges unprotected Responses API turn metadata after replacement; an empty patch removes nothing. */
@Serializable
@JvmInline
value class TurnMetadataPatch @ExperimentalCodexApi constructor(val entries: Map<String, String>) {
    override fun toString(): String = "TurnMetadataPatch(<redacted>)"
}
