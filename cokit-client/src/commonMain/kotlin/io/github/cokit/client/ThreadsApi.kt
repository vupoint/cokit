package io.github.cokit.client

import io.github.cokit.rpc.JsonRpcSession
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ThreadsApi internal constructor(
    private val rpc: JsonRpcSession,
) {
    suspend fun start(
        cwd: String? = null,
        approvalPolicy: String? = null,
        sandbox: String? = null,
        permissions: JsonElement? = null,
        model: String? = null,
        effort: String? = null,
        personality: String? = null,
    ): Thread {
        val result = rpc.request(
            method = "thread/start",
            params = buildJsonObject {
                cwd?.let { put("cwd", it) }
                approvalPolicy?.let { put("approvalPolicy", it) }
                sandbox?.let { put("sandbox", it) }
                permissions?.let { put("permissions", it) }
                model?.let { put("model", it) }
                effort?.let { put("effort", it) }
                personality?.let { put("personality", it) }
            },
        )
        return result.decodeResult<ThreadResult>().thread
    }

    suspend fun resume(
        threadId: String,
        excludeTurns: List<String> = emptyList(),
        initialTurnsPage: JsonElement? = null,
    ): Thread {
        val result = rpc.request(
            method = "thread/resume",
            params = buildJsonObject {
                put("threadId", threadId)
                if (excludeTurns.isNotEmpty()) {
                    put("excludeTurns", kotlinx.serialization.json.JsonArray(excludeTurns.map(::jsonString)))
                }
                initialTurnsPage?.let { put("initialTurnsPage", it) }
            },
        )
        return result.decodeResult<ThreadResult>().thread
    }

    suspend fun fork(
        threadId: String,
        ephemeral: Boolean? = null,
        excludeTurns: List<String> = emptyList(),
    ): Thread {
        val result = rpc.request(
            method = "thread/fork",
            params = buildJsonObject {
                put("threadId", threadId)
                ephemeral?.let { put("ephemeral", it) }
                if (excludeTurns.isNotEmpty()) {
                    put("excludeTurns", kotlinx.serialization.json.JsonArray(excludeTurns.map(::jsonString)))
                }
            },
        )
        return result.decodeResult<ThreadResult>().thread
    }

    suspend fun list(
        cursor: String? = null,
        limit: Int? = null,
        cwd: String? = null,
        archived: Boolean? = null,
        searchTerm: String? = null,
    ): ThreadList {
        val result = rpc.request(
            method = "thread/list",
            params = buildJsonObject {
                cursor?.let { put("cursor", it) }
                limit?.let { put("limit", it) }
                cwd?.let { put("cwd", it) }
                archived?.let { put("archived", it) }
                searchTerm?.let { put("searchTerm", it) }
            },
        )
        val decoded = result.decodeResult<ThreadListResult>()
        return ThreadList(decoded.threads, decoded.cursor)
    }

    suspend fun read(threadId: String, includeTurns: Boolean? = null): Thread {
        val result = rpc.request(
            method = "thread/read",
            params = buildJsonObject {
                put("threadId", threadId)
                includeTurns?.let { put("includeTurns", it) }
            },
        )
        return result.decodeResult<ThreadResult>().thread
    }

    suspend fun archive(threadId: String) {
        requestThreadMutation("thread/archive", threadId)
    }

    suspend fun unarchive(threadId: String) {
        requestThreadMutation("thread/unarchive", threadId)
    }

    suspend fun unsubscribe(threadId: String) {
        requestThreadMutation("thread/unsubscribe", threadId)
    }

    suspend fun setName(threadId: String, name: String) {
        rpc.request(
            method = "thread/name/set",
            params = buildJsonObject {
                put("threadId", threadId)
                put("name", name)
            },
        )
    }

    private suspend fun requestThreadMutation(method: String, threadId: String) {
        rpc.request(
            method = method,
            params = buildJsonObject { put("threadId", threadId) },
        )
    }
}

private fun jsonString(value: String) = kotlinx.serialization.json.JsonPrimitive(value)
