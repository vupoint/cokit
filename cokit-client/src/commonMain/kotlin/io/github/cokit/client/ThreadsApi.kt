package io.github.cokit.client

import io.github.cokit.protocol.CodexProtocolJson
import io.github.cokit.rpc.JsonRpcSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToJsonElement

class ThreadsApi internal constructor(
    private val rpc: JsonRpcSession,
) {
    suspend fun start(request: StartThreadRequest = StartThreadRequest()): Thread {
        val result = rpc.request(
            method = "thread/start",
            params = CodexProtocolJson.encodeToJsonElement(StartThreadRequest.serializer(), request),
        )
        return result.decodeResult<ThreadResult>().thread
    }

    suspend fun resume(request: ResumeThreadRequest): Thread {
        val result = rpc.request(
            method = "thread/resume",
            params = CodexProtocolJson.encodeToJsonElement(ResumeThreadRequest.serializer(), request),
        )
        return result.decodeResult<ThreadResult>().thread
    }

    suspend fun fork(request: ForkThreadRequest): Thread {
        val result = rpc.request(
            method = "thread/fork",
            params = CodexProtocolJson.encodeToJsonElement(ForkThreadRequest.serializer(), request),
        )
        return result.decodeResult<ThreadResult>().thread
    }

    suspend fun list(request: ListThreadsRequest = ListThreadsRequest()): ThreadList {
        val result = rpc.request(
            method = "thread/list",
            params = CodexProtocolJson.encodeToJsonElement(ListThreadsRequest.serializer(), request),
        )
        val decoded = result.decodeResult<ThreadListResult>()
        return ThreadList(decoded.threads, decoded.cursor)
    }

    suspend fun read(request: ReadThreadRequest): Thread {
        val result = rpc.request(
            method = "thread/read",
            params = CodexProtocolJson.encodeToJsonElement(ReadThreadRequest.serializer(), request),
        )
        return result.decodeResult<ThreadResult>().thread
    }

    suspend fun archive(threadId: ThreadId) {
        requestThreadMutation("thread/archive", threadId)
    }

    suspend fun unarchive(threadId: ThreadId) {
        requestThreadMutation("thread/unarchive", threadId)
    }

    suspend fun unsubscribe(threadId: ThreadId) {
        requestThreadMutation("thread/unsubscribe", threadId)
    }

    suspend fun setName(request: SetThreadNameRequest) {
        rpc.request(
            method = "thread/name/set",
            params = CodexProtocolJson.encodeToJsonElement(SetThreadNameRequest.serializer(), request),
        )
    }

    private suspend fun requestThreadMutation(method: String, threadId: ThreadId) {
        rpc.request(
            method = method,
            params = CodexProtocolJson.encodeToJsonElement(ThreadMutationRequest.serializer(), ThreadMutationRequest(threadId)),
        )
    }
}

@Serializable
private data class ThreadMutationRequest(
    val threadId: ThreadId,
)
