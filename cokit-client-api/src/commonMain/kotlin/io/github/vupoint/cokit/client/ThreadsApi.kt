package io.github.vupoint.cokit.client

interface ThreadsApi {
    suspend fun start(request: StartThreadRequest = StartThreadRequest()): Thread

    suspend fun resume(request: ResumeThreadRequest): Thread

    suspend fun fork(request: ForkThreadRequest): Thread

    suspend fun list(request: ListThreadsRequest = ListThreadsRequest()): ThreadList

    suspend fun read(request: ReadThreadRequest): Thread

    suspend fun archive(threadId: ThreadId)

    suspend fun unarchive(threadId: ThreadId)

    suspend fun unsubscribe(threadId: ThreadId)

    suspend fun setName(request: SetThreadNameRequest)
}
