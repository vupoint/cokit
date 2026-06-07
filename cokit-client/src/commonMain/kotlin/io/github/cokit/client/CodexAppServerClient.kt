package io.github.cokit.client

import io.github.cokit.protocol.CodexProtocolJson
import io.github.cokit.protocol.JsonRpcErrorObject
import io.github.cokit.protocol.JsonRpcNotification
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import io.github.cokit.rpc.JsonRpcSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

class CodexAppServerClient private constructor(
    private val rpc: JsonRpcSession,
    scope: CoroutineScope,
) : AutoCloseable {
    private val serverRequestHandlers =
        mutableMapOf<String, suspend (JsonRpcRequest) -> JsonElement?>()
    private val serverRequestJob: Job = scope.launch {
        rpc.serverRequests.collect { request ->
            rpc.sendResponse(resolveServerRequest(request))
        }
    }

    val threads: ThreadsApi = ThreadsApi(rpc)
    val turns: TurnsApi = TurnsApi(rpc)
    val rawEvents: SharedFlow<JsonRpcNotification> = rpc.notifications
    val events: Flow<CodexEvent> = rawEvents.map(CodexEvent::RawNotification)
    val isInitialized: Boolean = true

    override fun close() {
        serverRequestJob.cancel()
        rpc.close()
    }

    fun registerServerRequestHandler(
        method: String,
        handler: suspend (JsonRpcRequest) -> JsonElement?,
    ) {
        serverRequestHandlers[method] = handler
    }

    private suspend fun resolveServerRequest(request: JsonRpcRequest): JsonRpcResponse {
        val customHandler = serverRequestHandlers[request.method]
        if (customHandler != null) {
            return try {
                JsonRpcResponse(id = request.id, result = customHandler(request))
            } catch (error: Throwable) {
                JsonRpcResponse(
                    id = request.id,
                    error = JsonRpcErrorObject(
                        code = -32000,
                        message = error.message ?: "Server request handler failed",
                    ),
                )
            }
        }

        val defaultResult = defaultServerRequestResult(request.method)
        return if (defaultResult != null) {
            JsonRpcResponse(id = request.id, result = defaultResult)
        } else {
            JsonRpcResponse(
                id = request.id,
                error = JsonRpcErrorObject(
                    code = -32601,
                    message = "No handler registered for ${request.method}",
                ),
            )
        }
    }

    private fun defaultServerRequestResult(method: String): JsonElement? {
        return when (method) {
            "item/commandExecution/requestApproval",
            "item/fileChange/requestApproval",
            "item/permissions/requestApproval",
            "item/tool/call",
            "mcpServer/elicitation/request",
            -> buildJsonObject { put("decision", "decline") }

            "item/tool/requestUserInput" -> buildJsonObject { put("decision", "cancel") }
            "attestation/generate" -> buildJsonObject { put("status", "unsupported") }
            else -> null
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

sealed interface CodexEvent {
    data class RawNotification(val notification: JsonRpcNotification) : CodexEvent
}
