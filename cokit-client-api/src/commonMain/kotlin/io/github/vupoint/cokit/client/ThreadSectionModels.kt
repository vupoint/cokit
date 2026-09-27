package io.github.vupoint.cokit.client

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ThreadSectionId(val value: String)

@Serializable
data class ThreadSection(val id: ThreadSectionId, val name: String, val appearance: ThreadSectionAppearance? = null)

@Serializable
data class ThreadSectionAppearance(val color: String? = null, val icon: String? = null)

@Serializable
data class ThreadSectionListParams(val cursor: CodexCursor? = null, val limit: Int? = null)

@Serializable
data class ThreadSectionListResult(val data: List<ThreadSection>, val nextCursor: CodexCursor? = null)

@Serializable
data class ThreadSectionCreateParams(val name: String, val appearance: ThreadSectionAppearance? = null)

@Serializable
data class ThreadSectionResult(val section: ThreadSection)

@Serializable
data class ThreadSectionUpdateParams(
    val sectionId: ThreadSectionId,
    val name: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val appearance: CodexOptional<ThreadSectionAppearance> = CodexOptional.Omitted,
)

@Serializable
data class ThreadSectionDeleteParams(val sectionId: ThreadSectionId)

/** A null sectionId removes the thread from its section; a null beforeThreadId appends it. */
@Serializable
data class ThreadSectionMoveParams(
    val threadId: ThreadId,
    val sectionId: ThreadSectionId?,
    val beforeThreadId: ThreadId? = null,
)
