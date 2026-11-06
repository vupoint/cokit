package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.approvals.ApprovalDecision
import io.github.vupoint.cokit.protocol.CodexProtocolJson
import io.github.vupoint.cokit.protocol.JsonRpcId
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, ExperimentalCodexApi::class)
class HumanCommandApprovalTest {
    @Test
    fun humanCheckpointsNeverReachOrdinaryAutomaticApprovalHandlers() = runTest {
        for (capabilities in listOf(InitializeCapabilities(), humanCapabilities())) {
            val (client, transport) = connect(capabilities)
            var called = false
            client.registerCommandApprovalHandler { called = true; ApprovalDecision.Accept }
            transport.receive(humanRequest())
            runCurrent()
            assertFalse(called)
            assertEquals(json("""{"decision":"decline"}"""), (transport.sent.last() as JsonRpcResponse).result)
        }
    }

    @Test
    fun capabilityRequiresExperimentalInitializationBeforeAnyWireRequest() = runTest {
        for (enabled in listOf(false, true)) {
            val transport = FakeJsonRpcTransport()
            assertFailsWith<IllegalArgumentException> {
                CodexClients.connect(CodexClientConnection(
                    transport, ClientInfo("cokit_test", "CoKit Test", "0.1.0"), backgroundScope,
                    InitializeCapabilities(requiredHumanApproval = RequiredHumanApproval(enabled)),
                ))
            }
            assertTrue(transport.sent.isEmpty())
        }
    }

    @Test
    fun handlerRegistrationRequiresBothCapabilitiesAndDefaultRemainsDecline() = runTest {
        for (capabilities in listOf(
            InitializeCapabilities(),
            InitializeCapabilities(experimentalApi = true),
            InitializeCapabilities(experimentalApi = true, requiredHumanApproval = RequiredHumanApproval(false)),
        )) {
            val (client, _) = connect(capabilities)
            assertFailsWith<IllegalArgumentException> {
                client.registerHumanCommandApprovalHandler { ApprovalDecision.Accept }
            }
        }
        val (_, transport) = connect(humanCapabilities())
        transport.receive(humanRequest())
        runCurrent()
        assertEquals(json("""{"decision":"decline"}"""), (transport.sent.last() as JsonRpcResponse).result)
        val initialize = transport.sent.first() as JsonRpcRequest
        assertTrue(initialize.params.toString().contains("\"requiredHumanApproval\":true"))
    }

    @Test
    fun explicitHumanHandlerCanReturnOnlyOneTimeDecisionsAndReplaysAreNotCached() = runTest {
        val (client, transport) = connect(humanCapabilities())
        var calls = 0
        var choice: ApprovalDecision = ApprovalDecision.Accept
        client.registerHumanCommandApprovalHandler {
            calls++
            assertEquals("approval_1", it.approvalId?.value)
            choice
        }
        for ((decision, expected) in listOf(
            ApprovalDecision.Accept to "accept", ApprovalDecision.Decline to "decline",
            ApprovalDecision.Cancel to "cancel", ApprovalDecision.AcceptForSession to "decline",
        )) {
            choice = decision
            transport.receive(humanRequest(calls.toLong() + 100))
            runCurrent()
            assertEquals(json("""{"decision":"$expected"}"""), (transport.sent.last() as JsonRpcResponse).result)
        }
        assertEquals(4, calls)
        client.registerHumanCommandApprovalHandler { error("private handler details") }
        transport.receive(humanRequest())
        runCurrent()
        val response = transport.sent.last() as JsonRpcResponse
        assertEquals(-32000, response.error?.code)
        assertFalse(response.toString().contains("private handler details"))
    }

    @Test
    fun missingHumanPromptDetailsNeverReachTheHandler() = runTest {
        val (client, transport) = connect(humanCapabilities())
        var called = false
        client.registerHumanCommandApprovalHandler { called = true; ApprovalDecision.Accept }
        for (details in listOf("", ",\"approvalId\":\"approval_1\"", ",\"command\":\"example-command\"")) {
            transport.receive(JsonRpcRequest(JsonRpcId.Number(99), "item/commandExecution/requestApproval",
                json("""{"threadId":"thr_1","turnId":"turn_1","itemId":"item_1","startedAtMs":0,"kind":"humanCommand"$details}""")))
            runCurrent()
            assertFalse(called)
            assertEquals(-32602, (transport.sent.last() as JsonRpcResponse).error?.code)
        }
    }

    private fun humanCapabilities() = InitializeCapabilities(
        experimentalApi = true, requiredHumanApproval = RequiredHumanApproval(true),
    )

    private suspend fun TestScope.connect(
        capabilities: InitializeCapabilities = InitializeCapabilities(),
    ): Pair<CodexClient, FakeJsonRpcTransport> {
        val transport = FakeJsonRpcTransport()
        val pending = async { CodexClients.connect(CodexClientConnection(
            transport, ClientInfo("cokit_test", "CoKit Test", "0.1.0"), backgroundScope, capabilities,
        )) }
        runCurrent()
        val initialize = transport.sent.single() as JsonRpcRequest
        transport.receive(JsonRpcResponse(initialize.id, json("{}")))
        return pending.await() to transport
    }

    private fun humanRequest(id: Long = 99) = JsonRpcRequest(
        JsonRpcId.Number(id), "item/commandExecution/requestApproval",
        json("""{"threadId":"thr_1","turnId":"turn_1","itemId":"item_1","startedAtMs":1700000000000,"kind":"humanCommand","approvalId":"approval_1","command":"example-command"}"""),
    )

    private fun json(value: String) = CodexProtocolJson.parseToJsonElement(value)
}
