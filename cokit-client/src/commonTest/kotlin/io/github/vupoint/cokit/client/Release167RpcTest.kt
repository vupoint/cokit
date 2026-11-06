package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.auth.AccountSessionId
import io.github.vupoint.cokit.client.auth.AccountSessionListParams
import io.github.vupoint.cokit.client.auth.AccountSessionSwitchParams
import io.github.vupoint.cokit.client.auth.AccountSessionSwitchResult
import io.github.vupoint.cokit.client.auth.AccountWorkspaceId
import io.github.vupoint.cokit.client.auth.GcpSetupParams
import io.github.vupoint.cokit.client.extensions.HooksListParams
import io.github.vupoint.cokit.client.environment.CollaborationModeListParams
import io.github.vupoint.cokit.client.process.ProcessHandle
import io.github.vupoint.cokit.client.process.ProcessKillParams
import io.github.vupoint.cokit.client.process.ProcessResizePtyParams
import io.github.vupoint.cokit.client.process.ProcessSpawnParams
import io.github.vupoint.cokit.client.process.ProcessTerminalSize
import io.github.vupoint.cokit.client.process.ProcessWriteStdinParams
import io.github.vupoint.cokit.client.remote.RemoteControlClientId
import io.github.vupoint.cokit.client.remote.RemoteControlClientsListParams
import io.github.vupoint.cokit.client.remote.RemoteControlClientsRevokeParams
import io.github.vupoint.cokit.client.remote.RemoteControlDisableParams
import io.github.vupoint.cokit.client.remote.RemoteControlEnableParams
import io.github.vupoint.cokit.client.remote.RemoteControlEnvironmentId
import io.github.vupoint.cokit.client.remote.RemoteControlPairingStartParams
import io.github.vupoint.cokit.client.remote.RemoteControlPairingStatusParams
import io.github.vupoint.cokit.client.remote.RemoteControlStatusReadParams
import io.github.vupoint.cokit.protocol.CodexProtocolJson
import io.github.vupoint.cokit.protocol.JsonRpcErrorObject
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.rpc.JsonRpcRemoteException
import io.github.vupoint.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalCodexApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class Release167RpcTest {
    @Test
    fun existingExperimentalDescriptorsRejectBothOverloadsBeforeSending() = runTest {
        val fixture = connect()
        val process = ProcessHandle("proc_1")
        val environment = RemoteControlEnvironmentId("env_1")
        reject(fixture, CodexRpc.Process.Spawn, ProcessSpawnParams(listOf("echo", "example"), CodexHostPath("/path/to/project"), process))
        reject(fixture, CodexRpc.Process.WriteStdin, ProcessWriteStdinParams(process))
        reject(fixture, CodexRpc.Process.Kill, ProcessKillParams(process))
        reject(fixture, CodexRpc.Process.ResizePty, ProcessResizePtyParams(process, ProcessTerminalSize(80, 24)))
        reject(fixture, CodexRpc.CollaborationMode.List, CollaborationModeListParams)
        reject(fixture, CodexRpc.RemoteControl.Enable, RemoteControlEnableParams())
        reject(fixture, CodexRpc.RemoteControl.Disable, RemoteControlDisableParams())
        reject(fixture, CodexRpc.RemoteControl.ReadStatus, RemoteControlStatusReadParams)
        reject(fixture, CodexRpc.RemoteControl.StartPairing, RemoteControlPairingStartParams())
        reject(fixture, CodexRpc.RemoteControl.ReadPairingStatus, RemoteControlPairingStatusParams())
        reject(fixture, CodexRpc.RemoteControl.ListClients, RemoteControlClientsListParams(environment))
        reject(fixture, CodexRpc.RemoteControl.RevokeClient, RemoteControlClientsRevokeParams(environment, RemoteControlClientId("client_1")))
    }

    @Test
    fun newExperimentalDescriptorsRejectBothOverloadsBeforeSending() = runTest {
        val fixture = connect()
        reject(fixture, CodexRpc.AccountSession.List, AccountSessionListParams())
        reject(fixture, CodexRpc.AccountSession.Switch, AccountSessionSwitchParams(AccountSessionId("session_1"), AccountWorkspaceId("workspace_1")))
        reject(fixture, CodexRpc.Gcp.Setup, GcpSetupParams("example-project"))
        reject(fixture, CodexRpc.Thread.GuardianReview, ThreadGuardianReviewParams(ThreadId("thr_1")))
    }

    @Test
    fun sessionSwitchReturnsBrowserLoginOrSnapshotWithoutFollowingUpAutomatically() = runTest {
        val fixture = connect(experimental = true)
        val sessions = call(fixture, CodexRpc.AccountSession.List, AccountSessionListParams(refreshWorkspaceMetadata = true),
            "accountSession/list", """{"refreshWorkspaceMetadata":true}""", """{"activeSessionId":"session_1","sessions":[]}""")
        assertEquals(AccountSessionId("session_1"), sessions.activeSessionId)
        for (browserPayload in listOf(
            """{"loginId":"login_1","authUrl":"https://example.invalid/authorize"}""",
            """{"login_id":"login_1","auth_url":"https://example.invalid/authorize"}""",
        )) {
            val result = call(fixture, CodexRpc.AccountSession.Switch,
                AccountSessionSwitchParams(AccountSessionId("session_1"), AccountWorkspaceId("workspace_1"), allowBrowserLogin = true),
                "accountSession/switch", """{"sessionId":"session_1","accountId":"workspace_1","allowBrowserLogin":true}""", browserPayload)
            assertEquals("login_1", assertIs<AccountSessionSwitchResult.BrowserLogin>(result).loginId.value)
            assertFalse(result.toString().contains("example.invalid"))
        }
        val result = call(fixture, CodexRpc.AccountSession.Switch,
            AccountSessionSwitchParams(AccountSessionId("session_1"), AccountWorkspaceId("workspace_1")),
            "accountSession/switch", """{"sessionId":"session_1","accountId":"workspace_1"}""", """{"sessions":[]}""")
        assertEquals(emptyList(), assertIs<AccountSessionSwitchResult.Switched>(result).snapshot.sessions)
        assertEquals(6, fixture.second.sent.size)
    }

    @Test
    fun gcpSetupGuardianReviewAndSampleOptionsUseExactWireContracts() = runTest {
        val fixture = connect(experimental = true)
        call(fixture, CodexRpc.Gcp.Setup, GcpSetupParams("example-project", "global", CodexHostPath("/path/to/credentials.json")),
            "account/gcp/setup", """{"projectId":"example-project","location":"global","credentialFile":"/path/to/credentials.json"}""", "{}")
        call(fixture, CodexRpc.Gcp.Setup, GcpSetupParams("example-project"),
            "account/gcp/setup", """{"projectId":"example-project"}""", "{}")
        val review = call(fixture, CodexRpc.Thread.GuardianReview, ThreadGuardianReviewParams(ThreadId("thr_1")),
            "thread/guardianReview", """{"threadId":"thr_1"}""",
            """{"callId":"call_1","assessment":{"outcome":"deny","riskLevel":"high","userAuthorization":"low","rationale":"Example reason"}}""")
        assertEquals("deny", review.assessment?.outcome?.value)
        assertFalse(review.toString().contains("Example reason"))
        val skipped = call(fixture, CodexRpc.Thread.GuardianReview, ThreadGuardianReviewParams(ThreadId("thr_1")),
            "thread/guardianReview", """{"threadId":"thr_1"}""", """{"callId":"call_2","assessment":null}""")
        assertEquals(null, skipped.assessment)
        for (interrupt in listOf(false, true)) {
            call(fixture, CodexRpc.Thread.Sample,
                ThreadSampleParams(ThreadId("thr_1"), TurnId("turn_1"), "Example", interruptOnCancel = interrupt),
                "thread/sample", """{"threadId":"thr_1","expectedTurnId":"turn_1","prompt":"Example","interruptOnCancel":$interrupt}""",
                """{"sourceTurnId":"turn_1","responseId":"response_1"}""")
        }
        assertEquals(8, fixture.second.sent.size)
    }

    @Test
    fun hooksThreadScopeIsStableAndProviderErrorsPreserveDataWithoutRetry() = runTest {
        val fixture = connect()
        call(fixture, CodexRpc.Hooks.List, HooksListParams(threadId = ThreadId("thr_1")),
            "hooks/list", """{"threadId":"thr_1"}""", """{"data":[]}""")
        for (error in listOf(
            """{"type":"modelProviderMismatch","modelProvider":"example-old","currentModelProvider":"example-new"}""",
            """{"type":"modelProviderNotConfigured","modelProvider":"example"}""",
            """{"type":"modelProviderPolicyConflict","modelProvider":"example","requiredModelProvider":"managed"}""",
            """{"type":"futureProviderError","future":[1,true]}""",
        )) {
            val pending = async { runCatching { fixture.first.threads.resume(ResumeThreadRequest(ThreadId("thr_1"))) } }
            runCurrent()
            val request = fixture.second.sent.last() as JsonRpcRequest
            val count = fixture.second.sent.size
            fixture.second.receive(JsonRpcResponse(request.id, error = JsonRpcErrorObject(-32600, "Example rejection", json(error))))
            assertEquals(json(error), assertIs<JsonRpcRemoteException>(pending.await().exceptionOrNull()).error.data)
            runCurrent()
            assertEquals(count, fixture.second.sent.size)
        }
    }

    @Test
    fun setupAndSwitchErrorsNeverTriggerRetryOrAnotherLogin() = runTest {
        val fixture = connect(experimental = true)
        for (switch in listOf(false, true)) {
            val pending = async { runCatching {
                if (switch) fixture.first.request(CodexRpc.AccountSession.Switch,
                    AccountSessionSwitchParams(AccountSessionId("session_1"), AccountWorkspaceId("workspace_1")))
                else fixture.first.request(CodexRpc.Gcp.Setup, GcpSetupParams("example-project"))
            } }
            runCurrent()
            val request = fixture.second.sent.last() as JsonRpcRequest
            val count = fixture.second.sent.size
            val data = json("""{"reason":"example_rejection"}""")
            fixture.second.receive(JsonRpcResponse(request.id, error = JsonRpcErrorObject(-32600, "Example rejection", data)))
            assertEquals(data, assertIs<JsonRpcRemoteException>(pending.await().exceptionOrNull()).error.data)
            runCurrent()
            assertEquals(count, fixture.second.sent.size)
        }
    }

    private suspend fun <P : Any, R : Any> reject(fixture: Pair<CodexClient, FakeJsonRpcTransport>, method: CodexRpcMethod<P, R>, params: P) {
        val count = fixture.second.sent.size
        var callback = false
        assertFailsWith<IllegalArgumentException> { withTimeout(1_000) { fixture.first.request(method, params) } }
        assertFailsWith<IllegalArgumentException> { withTimeout(1_000) { fixture.first.request(method, params) { callback = true } } }
        assertFalse(callback)
        assertEquals(count, fixture.second.sent.size)
    }

    private suspend fun TestScope.connect(experimental: Boolean = false): Pair<CodexClient, FakeJsonRpcTransport> {
        val transport = FakeJsonRpcTransport()
        val pending = async { CodexClients.connect(CodexClientConnection(
            transport, ClientInfo("cokit_test", "CoKit Test", "0.1.0"), backgroundScope,
            InitializeCapabilities(experimentalApi = experimental),
        )) }
        runCurrent()
        transport.receive(JsonRpcResponse((transport.sent.single() as JsonRpcRequest).id, json("{}")))
        return pending.await() to transport
    }

    private suspend fun <P : Any, R : Any> TestScope.call(
        fixture: Pair<CodexClient, FakeJsonRpcTransport>, method: CodexRpcMethod<P, R>, params: P,
        expectedMethod: String, expectedParams: String, result: String,
    ): R {
        val pending = async { fixture.first.request(method, params) }
        runCurrent()
        val request = fixture.second.sent.last() as JsonRpcRequest
        assertEquals(expectedMethod, request.method)
        assertEquals(json(expectedParams), request.params)
        fixture.second.receive(JsonRpcResponse(request.id, json(result)))
        return pending.await()
    }

    private fun json(value: String) = CodexProtocolJson.parseToJsonElement(value)
}
