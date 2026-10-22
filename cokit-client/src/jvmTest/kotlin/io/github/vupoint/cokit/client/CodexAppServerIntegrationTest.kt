package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.transport.stdio.StdioCodexTransport
import io.github.vupoint.cokit.client.models.ModelProviderCapabilitiesReadParams
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class CodexAppServerIntegrationTest {
    @Test
    fun readsRelease163ContractsFromAnIsolatedRealAppServerWhenEnabled() = runTest {
        if (System.getenv("COKIT_CODEX_INTEGRATION") != "1") return@runTest

        val codexDirectory = Files.createTempDirectory("cokit-integration-").toFile()
        try {
            StdioCodexTransport(env = mapOf("CODEX_HOME" to codexDirectory.absolutePath)).use { transport ->
                val client = CodexClients.connect(CodexClientConnection(
                    transport, ClientInfo("cokit_integration", "CoKit Integration", "0.1.0"), backgroundScope,
                ))
                assertTrue(client.isInitialized)
                assertNotNull(client.request(CodexRpc.Model.ReadProviderCapabilities, ModelProviderCapabilitiesReadParams))
                val threads = client.threads.list(ListThreadsRequest(
                    limit = 1,
                    excludedThreadIds = listOf(ThreadId("019a0000-0000-7000-8000-000000000001")),
                ))
                assertEquals(emptyList(), threads.threads)
                val owners = client.request(CodexRpc.ThreadAttachmentOwner.List,
                    ThreadAttachmentOwnerListParams("example", "integration-fixture", limit = 1))
                assertEquals(emptyList(), owners.data)
            }
        } finally {
            codexDirectory.deleteRecursively()
        }
    }

    @Test
    fun initializesAndListsThreadsFromRealAppServerOverStdioWhenEnabled() = runTest {
        if (System.getenv("COKIT_CODEX_INTEGRATION") != "1") {
            return@runTest
        }

        StdioCodexTransport().use { transport ->
            val client = CodexClients.connect(
                CodexClientConnection(
                    transport = transport,
                    clientInfo = ClientInfo("cokit_integration", "CoKit Integration", "0.1.0"),
                    scope = backgroundScope,
                ),
            )

            assertTrue(client.isInitialized)
            val threads = client.threads.list(ListThreadsRequest(limit = 1))
            assertTrue(threads.threads.size <= 1)
        }
    }
}
