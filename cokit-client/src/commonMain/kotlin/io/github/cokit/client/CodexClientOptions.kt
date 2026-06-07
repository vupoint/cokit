package io.github.cokit.client

import io.github.cokit.rpc.JsonRpcTransport
import kotlinx.coroutines.CoroutineScope

data class CodexClientOptions(
    val transport: JsonRpcTransport,
    val clientInfo: ClientInfo,
    val scope: CoroutineScope,
    val capabilities: InitializeCapabilities? = null,
)
