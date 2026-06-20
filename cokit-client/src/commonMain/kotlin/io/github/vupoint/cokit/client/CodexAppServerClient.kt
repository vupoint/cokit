package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.protocol.CodexProtocolJson
import io.github.vupoint.cokit.protocol.JsonRpcErrorObject
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.rpc.JsonRpcSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.encodeToJsonElement

class CodexAppServerClient private constructor(
    private val rpc: JsonRpcSession,
    scope: CoroutineScope,
) : AutoCloseable {
    private val serverRequestHandlers =
        mutableMapOf<String, CodexServerRequestHandler>()
    private val serverRequestJob: Job = scope.launch {
        rpc.serverRequests.collect { request ->
            rpc.sendResponse(resolveServerRequest(request))
        }
    }

    val threads: ThreadsApi = ThreadsApi(rpc)
    val turns: TurnsApi = TurnsApi(rpc)
    val events: Flow<CodexEvent> = rpc.notifications.map { notification ->
        CodexEvent.Notification(
            method = notification.method,
            params = notification.params?.toCodexPayload(),
        )
    }
    val isInitialized: Boolean = true

    override fun close() {
        serverRequestJob.cancel()
        rpc.close()
    }

    fun registerServerRequestHandler(
        method: String,
        handler: CodexServerRequestHandler,
    ) {
        serverRequestHandlers[method] = handler
    }

    private suspend fun resolveServerRequest(request: JsonRpcRequest): JsonRpcResponse {
        val customHandler = serverRequestHandlers[request.method]
        if (customHandler != null) {
            return try {
                when (
                    val response = customHandler.respond(
                        CodexRawServerRequest(
                            method = request.method,
                            params = request.params?.toCodexPayload(),
                        ),
                    )
                ) {
                    is CodexServerResponse.Result -> {
                        JsonRpcResponse(id = request.id, result = response.payload.toJsonElement())
                    }
                    is CodexServerResponse.Error -> {
                        JsonRpcResponse(
                            id = request.id,
                            error = JsonRpcErrorObject(
                                code = response.code,
                                message = response.message,
                            ),
                        )
                    }
                }
            } catch (error: Throwable) {
                JsonRpcResponse(
                    id = request.id,
                    error = serverRequestHandlerError(),
                )
            }
        }

        val defaultResult = defaultServerRequestResult(request.method)
        return if (defaultResult != null) {
            JsonRpcResponse(id = request.id, result = defaultResult.toJsonElement())
        } else {
            JsonRpcResponse(
                id = request.id,
                error = noHandlerError(request.method),
            )
        }
    }

    companion object {
        suspend fun connect(options: CodexClientOptions): CodexAppServerClient {
            val session = JsonRpcSession(options.transport, options.scope)
            return try {
                val params = InitializeParams(
                    clientInfo = options.clientInfo,
                    capabilities = options.capabilities,
                )
                session.request(
                    method = "initialize",
                    params = CodexProtocolJson.encodeToJsonElement(InitializeParams.serializer(), params),
                )
                session.notify("initialized")
                CodexAppServerClient(session, options.scope)
            } catch (error: Throwable) {
                session.close()
                throw error
            }
        }
    }
}

fun interface CodexServerRequestHandler {
    suspend fun respond(request: CodexRawServerRequest): CodexServerResponse
}

sealed interface CodexServerResponse {
    data class Result(val payload: CodexJsonPayload? = null) : CodexServerResponse

    data class Error(
        val code: Int,
        val message: String,
    ) : CodexServerResponse
}

sealed interface CodexEvent {
    data class Notification(
        val method: String,
        val params: CodexJsonPayload? = null,
    ) : CodexEvent
}
