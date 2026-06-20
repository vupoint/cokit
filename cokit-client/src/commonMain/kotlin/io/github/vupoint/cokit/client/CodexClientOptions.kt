package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.rpc.JsonRpcTransport
import kotlinx.coroutines.CoroutineScope

data class CodexClientOptions(
    val transport: JsonRpcTransport,
    val clientInfo: ClientInfo,
    val scope: CoroutineScope,
    val capabilities: InitializeCapabilities? = null,
)
