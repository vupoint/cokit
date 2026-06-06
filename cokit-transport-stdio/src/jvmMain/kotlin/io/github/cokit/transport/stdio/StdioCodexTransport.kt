package io.github.cokit.transport.stdio

import io.github.cokit.protocol.JsonRpcMessage
import io.github.cokit.rpc.JsonRpcTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class StdioCodexTransport(
    val command: List<String> = listOf("codex", "app-server", "--stdio"),
) : JsonRpcTransport {
    override val incoming: Flow<JsonRpcMessage> = emptyFlow()

    override suspend fun send(message: JsonRpcMessage) {
        error("Stdio transport framing is not implemented yet")
    }

    override fun close() = Unit
}
