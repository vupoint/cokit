package io.github.cokit.client

import kotlinx.serialization.Serializable

@Serializable
data class ThreadId(val value: String)

@Serializable
data class Thread(
    val id: String,
    val preview: String? = null,
    val modelProvider: String? = null,
    val createdAt: Long? = null,
)

@Serializable
data class ThreadList(
    val threads: List<Thread> = emptyList(),
    val cursor: String? = null,
)

@Serializable
internal data class ThreadResult(
    val thread: Thread,
)

@Serializable
internal data class ThreadListResult(
    val threads: List<Thread> = emptyList(),
    val cursor: String? = null,
)
