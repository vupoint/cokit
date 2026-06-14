package io.github.cokit.client

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ItemId(val value: String)

@Serializable
@JvmInline
value class ItemStatus(val value: String) {
    companion object {
        val InProgress = ItemStatus("inProgress")
        val Completed = ItemStatus("completed")
        val Failed = ItemStatus("failed")
        val Declined = ItemStatus("declined")
    }
}
