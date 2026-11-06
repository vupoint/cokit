package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.transport.stdio.StdioCodexTransport
import io.github.vupoint.cokit.client.models.ModelProviderCapabilitiesReadParams
import io.github.vupoint.cokit.client.auth.AccountSessionListParams
import io.github.vupoint.cokit.client.extensions.HooksListParams
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith
import io.github.vupoint.cokit.rpc.JsonRpcRemoteException
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

class CodexAppServerIntegrationTest {
    @OptIn(ExperimentalCodexApi::class)
    @Test
    fun readsRelease167ContractsAndSectionEventsFromAnIsolatedRealAppServerWhenEnabled() = runTest {
        if (System.getenv("COKIT_CODEX_INTEGRATION") != "1") return@runTest
        val directory = Files.createTempDirectory("cokit-release-167-").toFile()
        try {
            directory.resolve("codex-home").mkdirs()
            StdioCodexTransport(env = mapOf("CODEX_HOME" to directory.resolve("codex-home").absolutePath)).use { transport ->
                val client = CodexClients.connect(CodexClientConnection(
                    transport, ClientInfo("cokit_integration", "CoKit Integration", "0.1.0"), backgroundScope,
                    InitializeCapabilities(experimentalApi = true, requiredHumanApproval = RequiredHumanApproval(true)),
                ))
                val sessions = client.request(CodexRpc.AccountSession.List, AccountSessionListParams())
                assertEquals(emptyList(), sessions.sessions)
                val thread = client.threads.start(StartThreadRequest(cwd = CodexHostPath(directory.absolutePath)))
                assertNotNull(client.request(CodexRpc.Hooks.List, HooksListParams(threadId = thread.id)))
                val section = client.request(CodexRpc.ThreadSection.Create, ThreadSectionCreateParams("Example section")).section
                for (destination in listOf(section.id, section.id, null)) {
                    val event = async(start = CoroutineStart.UNDISPATCHED) {
                        client.notifications.filterIsInstance<CodexNotification.ThreadSectionChanged>().first()
                    }
                    client.request(CodexRpc.Thread.MoveToSection, ThreadSectionMoveParams(thread.id, destination))
                    assertEquals(CodexNotification.ThreadSectionChanged(thread.id, destination), event.await())
                }
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    @OptIn(ExperimentalCodexApi::class)
    @Test
    fun readsRelease165ContractsAndSavesFeedbackFromAnIsolatedRealAppServerWhenEnabled() = runTest {
        if (System.getenv("COKIT_CODEX_INTEGRATION") != "1") return@runTest
        val version = ProcessBuilder("codex", "--version").start().inputStream.bufferedReader().use { it.readText().trim() }
        assertEquals("codex-cli 0.167.0", version)
        val directory = Files.createTempDirectory("cokit-release-165-").toFile()
        try {
            directory.resolve("codex-home").mkdirs()
            StdioCodexTransport(env = mapOf("CODEX_HOME" to directory.resolve("codex-home").absolutePath)).use { transport ->
                val client = CodexClients.connect(CodexClientConnection(
                    transport, ClientInfo("cokit_integration", "CoKit Integration", "0.1.0"), backgroundScope,
                    InitializeCapabilities(experimentalApi = true),
                ))
                val thread = client.threads.start(StartThreadRequest(cwd = CodexHostPath(directory.absolutePath)))
                val memory = client.request(CodexRpc.Thread.ReadMemory, ThreadMemoryReadParams(thread.id))
                assertEquals(null, memory.summary)
                val attached = client.threads.list(ListThreadsRequest(attachedToThreadId = ThreadAttachedTo(thread.id)))
                assertEquals(emptyList(), attached.threads)
                val path = directory.resolve("feedback.zip")
                val saved = client.request(CodexRpc.Feedback.Save,
                    FeedbackSaveParams("example", CodexHostPath(path.absolutePath), threadId = thread.id, includeLogs = false))
                assertEquals(CodexHostPath(path.absolutePath), saved.savedPath)
                assertTrue(path.isFile)
                val original = path.readBytes().toList()
                assertFailsWith<JsonRpcRemoteException> {
                    client.request(CodexRpc.Feedback.Save,
                        FeedbackSaveParams("example", CodexHostPath(path.absolutePath), threadId = thread.id))
                }
                assertEquals(original, path.readBytes().toList())
            }
        } finally {
            directory.deleteRecursively()
        }
    }

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
