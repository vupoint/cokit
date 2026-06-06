package io.github.cokit.client

import io.github.cokit.protocol.CodexProtocolJson
import io.github.cokit.protocol.JsonRpcNotification
import io.github.cokit.rpc.JsonRpcSession
import io.github.cokit.rpc.JsonRpcTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.encodeToJsonElement

class CodexAppServerClient private constructor(
    val rpc: JsonRpcSession,
) : AutoCloseable {
    val threads: ThreadsApi = ThreadsApi(rpc)
    val turns: TurnsApi = TurnsApi(rpc)
    val rawEvents: SharedFlow<JsonRpcNotification> = rpc.notifications
    val events: Flow<CodexEvent> = rawEvents.map(CodexEvent::RawNotification)
    val isInitialized: Boolean = true

    override fun close() {
        rpc.close()
    }

    companion object {
        suspend fun connect(
            transport: JsonRpcTransport,
            clientInfo: ClientInfo,
            scope: CoroutineScope,
            capabilities: InitializeCapabilities? = null,
        ): CodexAppServerClient {
            val session = JsonRpcSession(transport, scope)
            return try {
                val params = InitializeParams(
                    clientInfo = clientInfo,
                    capabilities = capabilities,
                )
                session.request(
                    method = "initialize",
                    params = CodexProtocolJson.encodeToJsonElement(InitializeParams.serializer(), params),
                )
                session.notify("initialized")
                CodexAppServerClient(session)
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
