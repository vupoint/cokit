@file:OptIn(ExperimentalCodexApi::class)

package io.github.vupoint.cokit.client

internal class DefaultThreadsApi(
    private val requests: ClientRequestExecutor,
) : ThreadsApi {
    override suspend fun start(request: StartThreadRequest): Thread {
        val params = request.toRpcParams()
        return requests.request(CodexRpc.Thread.Start, params).thread
    }

    override suspend fun resume(request: ResumeThreadRequest): Thread {
        return requests.request(CodexRpc.Thread.Resume, request.toRpcParams()).thread
    }

    override suspend fun fork(request: ForkThreadRequest): Thread {
        val params = request.toRpcParams()
        return requests.request(CodexRpc.Thread.Fork, params).thread
    }

    override suspend fun list(request: ListThreadsRequest): ThreadList {
        val params = request.toRpcParams()
        val decoded = requests.request(CodexRpc.Thread.List, params)
        return ThreadList(decoded.threads, decoded.nextCursor, decoded.backwardsCursor, decoded.readStates)
    }

    override suspend fun listLoaded(request: ListLoadedThreadsRequest): LoadedThreadList {
        val decoded = requests.request(
            CodexRpc.Thread.ListLoaded,
            ThreadLoadedListParams(request.cursor, request.limit),
        )
        return LoadedThreadList(decoded.threadIds, decoded.nextCursor)
    }

    override suspend fun read(request: ReadThreadRequest): Thread {
        return requests.request(CodexRpc.Thread.Read, request.toRpcParams()).thread
    }

    override suspend fun archive(threadId: ThreadId) {
        requests.request(CodexRpc.Thread.Archive, ThreadArchiveParams(threadId))
    }

    override suspend fun unarchive(threadId: ThreadId): Thread {
        return requests.request(CodexRpc.Thread.Unarchive, ThreadUnarchiveParams(threadId)).thread
    }

    override suspend fun unsubscribe(threadId: ThreadId) {
        requests.request(CodexRpc.Thread.Unsubscribe, ThreadUnsubscribeParams(threadId))
    }

    override suspend fun setName(request: SetThreadNameRequest) {
        requests.request(CodexRpc.Thread.SetName, request.toRpcParams())
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
    dynamicTools = dynamicTools,
)

private fun ResumeThreadRequest.toRpcParams(): ThreadResumeParams = ThreadResumeParams(
    threadId = threadId,
    excludeTurns = excludeTurns,
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
    excludeTurns = excludeTurns,
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
    experimentalPredictionMode = experimentalPredictionMode,
    attachToSource = attachToSource,
    experimentalDisablePredictionMcp = experimentalDisablePredictionMcp,
)

private fun ListThreadsRequest.toRpcParams(): ThreadListParams = ThreadListParams(
    sourceKinds = sourceKinds,
    archived = archived,
    cursor = cursor,
    cwd = cwd,
    sectionId = sectionId,
    originators = originators,
    limit = limit,
    modelProviders = modelProviders,
    useStateDbOnly = useStateDbOnly,
    searchTerm = searchTerm,
    sortDirection = sortDirection,
    sortKey = sortKey,
    excludedThreadIds = excludedThreadIds,
    attachedToThreadId = attachedToThreadId,
)

private fun ReadThreadRequest.toRpcParams(): ThreadReadParams = ThreadReadParams(
    threadId = threadId,
    includeTurns = includeTurns,
)

private fun SetThreadNameRequest.toRpcParams(): ThreadSetNameParams = ThreadSetNameParams(
    threadId = threadId,
    name = name,
)
