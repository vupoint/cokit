package io.github.vupoint.cokit.client

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ThreadAttachmentId(val value: String)

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

@Serializable
data class ThreadAttachmentAddResult(val attachment: ThreadAttachment, val outcome: ThreadAttachmentAddOutcome)

@Serializable
@JvmInline
value class ThreadAttachmentAddOutcome(val value: String) {
    companion object {
        val Created = ThreadAttachmentAddOutcome("created")
        val Existing = ThreadAttachmentAddOutcome("existing")
    }
}

@Serializable
data class ThreadAttachmentListParams(val threadId: ThreadId, val cursor: CodexCursor? = null, val limit: Int? = null)

@Serializable
data class ThreadAttachmentListResult(val data: List<ThreadAttachment>, val nextCursor: CodexCursor? = null)

/** Removes the attachment reference, not its external resource. */
@Serializable
data class ThreadAttachmentRemoveParams(val threadId: ThreadId, val attachmentType: String, val identityKey: String)

@Serializable
@JvmInline
value class ThreadAttachmentOperation(val value: String) {
    companion object {
        val Created = ThreadAttachmentOperation("created")
        val Deleted = ThreadAttachmentOperation("deleted")
    }
}
