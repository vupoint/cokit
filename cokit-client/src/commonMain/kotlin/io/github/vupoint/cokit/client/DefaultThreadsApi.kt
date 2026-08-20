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
        return ThreadList(decoded.threads, decoded.nextCursor, decoded.backwardsCursor)
    }

    override suspend fun listLoaded(request: ListLoadedThreadsRequest): LoadedThreadList {
        val decoded = rpc.request(
            CodexRpc.Thread.ListLoaded,
            ThreadLoadedListParams(request.cursor, request.limit),
        )
        return LoadedThreadList(decoded.threadIds, decoded.nextCursor)
    }

    override suspend fun read(request: ReadThreadRequest): Thread {
        return rpc.request(CodexRpc.Thread.Read, request.toRpcParams()).thread
    }

    override suspend fun archive(threadId: ThreadId) {
        rpc.request(CodexRpc.Thread.Archive, ThreadArchiveParams(threadId))
    }

    override suspend fun unarchive(threadId: ThreadId): Thread {
        return rpc.request(CodexRpc.Thread.Unarchive, ThreadUnarchiveParams(threadId)).thread
    }

    override suspend fun unsubscribe(threadId: ThreadId) {
        rpc.request(CodexRpc.Thread.Unsubscribe, ThreadUnsubscribeParams(threadId))
    }

    override suspend fun setName(request: SetThreadNameRequest) {
        rpc.request(CodexRpc.Thread.SetName, request.toRpcParams())
    }
}

private fun StartThreadRequest.toRpcParams(): ThreadStartParams = ThreadStartParams(
    serviceTier = serviceTier,
    cwd = cwd,
    approvalPolicy = approvalPolicy,
    approvalsReviewer = approvalsReviewer,
    baseInstructions = baseInstructions,
    config = config,
    developerInstructions = developerInstructions,
    serviceName = serviceName,
    sessionStartSource = sessionStartSource,
    ephemeral = ephemeral,
    sandbox = sandbox,
    threadSource = threadSource,
    model = model,
    modelProvider = modelProvider,
    personality = personality,
)

private fun ResumeThreadRequest.toRpcParams(): ThreadResumeParams = ThreadResumeParams(
    threadId = threadId,
    approvalPolicy = approvalPolicy,
    approvalsReviewer = approvalsReviewer,
    baseInstructions = baseInstructions,
    config = config,
    cwd = cwd,
    developerInstructions = developerInstructions,
    personality = personality,
    sandbox = sandbox,
    model = model,
    modelProvider = modelProvider,
    serviceTier = serviceTier,
)

private fun ForkThreadRequest.toRpcParams(): ThreadForkParams = ThreadForkParams(
    threadId = threadId,
    approvalPolicy = approvalPolicy,
    approvalsReviewer = approvalsReviewer,
    baseInstructions = baseInstructions,
    config = config,
    cwd = cwd,
    sandbox = sandbox,
    developerInstructions = developerInstructions,
    ephemeral = ephemeral,
    threadSource = threadSource,
    lastTurnId = lastTurnId,
    model = model,
    modelProvider = modelProvider,
    serviceTier = serviceTier,
)

private fun ListThreadsRequest.toRpcParams(): ThreadListParams = ThreadListParams(
    sourceKinds = sourceKinds,
    archived = archived,
    cursor = cursor,
    cwd = cwd,
    isPinned = isPinned,
    limit = limit,
    modelProviders = modelProviders,
    useStateDbOnly = useStateDbOnly,
    searchTerm = searchTerm,
    sortDirection = sortDirection,
    sortKey = sortKey,
)

private fun ReadThreadRequest.toRpcParams(): ThreadReadParams = ThreadReadParams(
    threadId = threadId,
    includeTurns = includeTurns,
)

private fun SetThreadNameRequest.toRpcParams(): ThreadSetNameParams = ThreadSetNameParams(
    threadId = threadId,
    name = name,
)
