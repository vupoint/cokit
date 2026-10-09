package io.github.vupoint.cokit.client

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

/** Opaque server identifier for a thread-organizing section. */
@Serializable
@JvmInline
value class ThreadSectionId(val value: String)

/** Named server section with optional appearance metadata. */
@Serializable
data class ThreadSection(val id: ThreadSectionId, val name: String, val appearance: ThreadSectionAppearance? = null)

/** Server section color and icon metadata; values are passed through without rendering UI. */
@Serializable
data class ThreadSectionAppearance(val color: String? = null, val icon: String? = null)

/** Pagination for server sections; null options use server defaults. */
@Serializable
data class ThreadSectionListParams(val cursor: CodexCursor? = null, val limit: Int? = null)

/** Page of sections; reuse [nextCursor] unchanged to fetch the next page. */
@Serializable
data class ThreadSectionListResult(val data: List<ThreadSection>, val nextCursor: CodexCursor? = null)

/** Creates a named section with optional appearance metadata. */
@Serializable
data class ThreadSectionCreateParams(val name: String, val appearance: ThreadSectionAppearance? = null)

/** Section returned by a create or update operation. */
@Serializable
data class ThreadSectionResult(val section: ThreadSection)

/**
 * Renames a section and optionally changes its appearance.
 *
 * Omitting [appearance] preserves it, an explicit null clears it, and a value replaces it.
 */
@Serializable
data class ThreadSectionUpdateParams(
    val sectionId: ThreadSectionId,
    val name: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val appearance: CodexOptional<ThreadSectionAppearance> = CodexOptional.Omitted,
)

/** Identifies the section to delete from the server. */
@Serializable
data class ThreadSectionDeleteParams(val sectionId: ThreadSectionId)

/** A null sectionId removes the thread from its section; a null beforeThreadId appends it. */
@Serializable
data class ThreadSectionMoveParams(
    val threadId: ThreadId,
    val sectionId: ThreadSectionId?,
    val beforeThreadId: ThreadId? = null,
)
