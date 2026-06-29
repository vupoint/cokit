package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.rpc.JsonRpcSession

internal class DefaultThreadsApi(
    private val rpc: JsonRpcSession,
) : ThreadsApi {
    override suspend fun start(request: StartThreadRequest): Thread {
        return rpc.request(CodexRpc.Thread.Start, request.toRpcParams()).thread
    }

    override suspend fun resume(request: ResumeThreadRequest): Thread {
        return rpc.request(CodexRpc.Thread.Resume, request.toRpcParams()).thread
    }

    override suspend fun fork(request: ForkThreadRequest): Thread {
        return rpc.request(CodexRpc.Thread.Fork, request.toRpcParams()).thread
    }

    override suspend fun list(request: ListThreadsRequest): ThreadList {
        val decoded = rpc.request(CodexRpc.Thread.List, request.toRpcParams())
        return ThreadList(decoded.threads, decoded.cursor)
    }

    override suspend fun read(request: ReadThreadRequest): Thread {
        return rpc.request(CodexRpc.Thread.Read, request.toRpcParams()).thread
    }

    override suspend fun archive(threadId: ThreadId) {
        rpc.request(CodexRpc.Thread.Archive, ThreadArchiveParams(threadId))
    }

    override suspend fun unarchive(threadId: ThreadId) {
        rpc.request(CodexRpc.Thread.Unarchive, ThreadUnarchiveParams(threadId))
    }

    override suspend fun unsubscribe(threadId: ThreadId) {
        rpc.request(CodexRpc.Thread.Unsubscribe, ThreadUnsubscribeParams(threadId))
    }

    override suspend fun setName(request: SetThreadNameRequest) {
        rpc.request(CodexRpc.Thread.SetName, request.toRpcParams())
    }
}

private fun StartThreadRequest.toRpcParams(): ThreadStartParams = ThreadStartParams(
    cwd = cwd,
    approvalPolicy = approvalPolicy,
    sandbox = sandbox,
    permissions = permissions,
    model = model,
    effort = effort,
    personality = personality,
)

private fun ResumeThreadRequest.toRpcParams(): ThreadResumeParams = ThreadResumeParams(
    threadId = threadId,
    excludeTurns = excludeTurns,
    initialTurnsPage = initialTurnsPage,
)

private fun ForkThreadRequest.toRpcParams(): ThreadForkParams = ThreadForkParams(
    threadId = threadId,
    ephemeral = ephemeral,
    excludeTurns = excludeTurns,
)

private fun ListThreadsRequest.toRpcParams(): ThreadListParams = ThreadListParams(
    cursor = cursor,
    limit = limit,
    cwd = cwd,
    archived = archived,
    searchTerm = searchTerm,
)

private fun ReadThreadRequest.toRpcParams(): ThreadReadParams = ThreadReadParams(
    threadId = threadId,
    includeTurns = includeTurns,
)

private fun SetThreadNameRequest.toRpcParams(): ThreadSetNameParams = ThreadSetNameParams(
    threadId = threadId,
    name = name,
)
