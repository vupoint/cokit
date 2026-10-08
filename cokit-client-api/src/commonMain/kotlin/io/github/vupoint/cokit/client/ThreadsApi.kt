package io.github.vupoint.cokit.client

/**
 * Thread lifecycle helpers exposed by [CodexClient.threads].
 *
 * These operations return thread models directly. Use [CodexClient.request] with [CodexRpc.Thread]
 * for additional operations and complete wire result metadata. Nullable request options are omitted
 * from the wire; the server determines their meaning. Cancelling a coroutine does not cancel the
 * server operation.
 */
interface ThreadsApi {
    /** Creates a thread; starting a thread does not start a turn. */
    suspend fun start(request: StartThreadRequest = StartThreadRequest()): Thread

    /** Resumes the identified thread with the supplied configuration overrides. */
    suspend fun resume(request: ResumeThreadRequest): Thread

    /** Creates a separate thread from the source thread's history. */
    suspend fun fork(request: ForkThreadRequest): Thread

    /** Returns one filtered page; pass [ThreadList.nextCursor] to fetch a subsequent page. */
    suspend fun list(request: ListThreadsRequest = ListThreadsRequest()): ThreadList

    /** Returns one page of IDs currently loaded in the app-server, rather than stored history. */
    suspend fun listLoaded(request: ListLoadedThreadsRequest = ListLoadedThreadsRequest()): LoadedThreadList

    /** Reads stored thread metadata and, when requested, its turns. */
    suspend fun read(request: ReadThreadRequest): Thread

    /** Archives the identified thread through the server's thread lifecycle API. */
    suspend fun archive(threadId: ThreadId)

    /** Restores an archived thread and returns the server's thread snapshot. */
    suspend fun unarchive(threadId: ThreadId): Thread

    /** Detaches this connection from the thread's subscription without deleting its history. */
    suspend fun unsubscribe(threadId: ThreadId)

    /** Sets the thread's user-visible name. */
    suspend fun setName(request: SetThreadNameRequest)
}
