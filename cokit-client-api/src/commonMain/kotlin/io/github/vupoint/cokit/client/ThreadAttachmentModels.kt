package io.github.vupoint.cokit.client

import kotlinx.serialization.Serializable

/** Opaque server identifier for an attachment reference. */
@Serializable
@JvmInline
value class ThreadAttachmentId(val value: String)

/**
 * Resource reference attached to a conversation.
 *
 * [payload] preserves type-specific JSON data. [identityKey] identifies the resource
 * within [attachmentType]; [createdAt] is expressed in Unix seconds.
 */
@Serializable
data class ThreadAttachment(
    val id: ThreadAttachmentId,
    val attachmentType: String,
    val identityKey: String,
    val payload: CodexJsonPayload,
    val createdAt: CodexTimestamp,
)

/** Adds a resource reference, deduplicated by attachment type and identity key. */
@Serializable
data class ThreadAttachmentAddParams(
    val threadId: ThreadId,
    val attachmentType: String,
    val identityKey: String,
    val payload: CodexJsonPayload,
)

/** Attachment returned after deduplication, with [outcome] indicating creation or reuse. */
@Serializable
data class ThreadAttachmentAddResult(val attachment: ThreadAttachment, val outcome: ThreadAttachmentAddOutcome)

/** Whether an add operation created a reference or returned an existing matching reference. */
@Serializable
@JvmInline
value class ThreadAttachmentAddOutcome(val value: String) {
    companion object {
        val Created = ThreadAttachmentAddOutcome("created")
        val Existing = ThreadAttachmentAddOutcome("existing")
    }
}

/** Pagination for attachment references belonging to [threadId]. */
@Serializable
data class ThreadAttachmentListParams(val threadId: ThreadId, val cursor: CodexCursor? = null, val limit: Int? = null)

/** Page of attachment references with an optional opaque continuation token. */
@Serializable
data class ThreadAttachmentListResult(val data: List<ThreadAttachment>, val nextCursor: CodexCursor? = null)

/** Removes the attachment reference, not its external resource. */
@Serializable
data class ThreadAttachmentRemoveParams(val threadId: ThreadId, val attachmentType: String, val identityKey: String)

/** Change kind reported by an attachment-update notification. */
@Serializable
@JvmInline
value class ThreadAttachmentOperation(val value: String) {
    companion object {
        val Created = ThreadAttachmentOperation("created")
        val Deleted = ThreadAttachmentOperation("deleted")
    }
}
