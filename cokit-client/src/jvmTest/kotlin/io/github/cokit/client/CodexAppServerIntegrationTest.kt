package io.github.cokit.client

import io.github.cokit.transport.stdio.StdioCodexTransport
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class CodexAppServerIntegrationTest {
    @Test
    fun initializesRealAppServerOverStdioWhenEnabled() = runTest {
        if (System.getenv("COKIT_CODEX_INTEGRATION") != "1") {
            return@runTest
        }

        StdioCodexTransport().use { transport ->
            val client = CodexRpcClient.connect(
                CodexRpcConnection(
                    transport = transport,
                    clientInfo = ClientInfo("cokit_integration", "CoKit Integration", "0.1.0"),
                    scope = backgroundScope,
                ),
            )

            assertTrue(client.isInitialized)
        }
    }
}
