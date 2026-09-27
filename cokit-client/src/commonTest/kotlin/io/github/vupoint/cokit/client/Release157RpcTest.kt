package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.protocol.CodexProtocolJson
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
class Release157RpcTest {
    @Test
    fun attachmentMethodsUseExplicitWireContracts() = runTest {
        val fixture = connect()
        val attachment = """{"id":"att_1","attachmentType":"pull_request","identityKey":"example","payload":{"future":true},"createdAt":1}"""
        val added = call(fixture, CodexRpc.ThreadAttachment.Add,
            ThreadAttachmentAddParams(ThreadId("thr_1"), "pull_request", "example", CodexJsonPayload.parse("""{"future":true}""")),
            """{"threadId":"thr_1","attachmentType":"pull_request","identityKey":"example","payload":{"future":true}}""",
            """{"attachment":$attachment,"outcome":"existing"}""")
        assertEquals(ThreadAttachmentAddOutcome.Existing, added.outcome)
        val listed = call(fixture, CodexRpc.ThreadAttachment.List, ThreadAttachmentListParams(ThreadId("thr_1"), limit = 1),
            """{"threadId":"thr_1","limit":1}""", """{"data":[$attachment],"nextCursor":"next"}""")
        assertEquals(CodexCursor("next"), listed.nextCursor)
        call(fixture, CodexRpc.ThreadAttachment.Remove, ThreadAttachmentRemoveParams(ThreadId("thr_1"), "pull_request", "example"),
            """{"threadId":"thr_1","attachmentType":"pull_request","identityKey":"example"}""", "{}")
    }

    @Test
    fun gatewayMethodsSendNoParamsAndRedactSensitiveDiagnostics() = runTest {
        val fixture = connect()
        val read = call(fixture, CodexRpc.GatewayOAuth.Read, CodexRpcUnit, null,
            """{"providerId":"example","providerName":"Example","required":true,"status":"notReady","error":"private-diagnostic"}""")
        assertEquals(true, read.required)
        assertFalse(read.toString().contains("private-diagnostic"))
        call(fixture, CodexRpc.GatewayOAuth.Login, CodexRpcUnit, null, "{}")
        call(fixture, CodexRpc.GatewayOAuth.Cancel, CodexRpcUnit, null, "{}")
        val event = CodexProtocolJson.decodeFromString(CodexNotification.GatewayOAuthChanged.serializer(),
            """{"providerId":"example","status":"started","authUrl":"https://example.invalid/private-auth","error":"private-diagnostic"}""")
        assertFalse(event.toString().contains("private-auth"))
        assertFalse(event.toString().contains("private-diagnostic"))
    }

    @Test
    fun experimentalOperationsRequireInitializationOptInBeforeSending() = runTest {
        val fixture = connect()
        val before = fixture.second.sent.size
        assertFailsWith<IllegalArgumentException> {
            fixture.first.request(CodexRpc.Project.List, ProjectListParams())
        }
        assertFailsWith<IllegalArgumentException> {
            fixture.first.request(CodexRpc.ThreadQueue.Start, ThreadQueueStartParams(ThreadId("thr_1"))) {}
        }
        assertFailsWith<IllegalArgumentException> {
            fixture.first.request(CodexRpc.UserVerification.Enroll, CodexRpcUnit)
        }
        assertEquals(before, fixture.second.sent.size)
    }

    @Test
    fun projectAndQueueOperationsPreservePaginationAndOpaqueInput() = runTest {
        val fixture = connect(experimental = true)
        val project = """{"id":"prj_1","name":"Example","roots":[{"path":"/path/to/project"}],"metadata":{"source":"example"},"position":1,"createdAt":1,"updatedAt":2,"recencyAt":null}"""
        val imported = call(fixture, CodexRpc.Project.Import,
            ProjectImportParams("import_1", "Example", listOf(ProjectRoot(CodexHostPath("/path/to/project"))), threads = listOf(ThreadId("thr_1"))),
            """{"idempotencyKey":"import_1","name":"Example","roots":[{"path":"/path/to/project"}],"threads":["thr_1"]}""", """{"project":$project}""")
        assertEquals("prj_1", imported.project.id)
        val list = call(fixture, CodexRpc.Project.List, ProjectListParams(limit = 1, sortKey = ProjectSortKey.RecencyAt),
            """{"limit":1,"sortKey":"recencyAt"}""", """{"data":[$project],"nextCursor":"next"}""")
        assertEquals(CodexCursor("next"), list.nextCursor)
        val input = TurnInput.Custom(CodexJsonPayload.parse("""{"type":"futureInput","value":true}"""))
        val added = call(fixture, CodexRpc.ThreadQueue.Add, ThreadQueueAddParams(ThreadId("thr_1"), "msg_1", listOf(input)),
            """{"threadId":"thr_1","clientUserMessageId":"msg_1","input":[{"type":"futureInput","value":true}]}""",
            """{"queuedSubmission":{"id":"queue_1","clientUserMessageId":"msg_1","input":[{"type":"futureInput","value":true}]}}""")
        assertEquals(listOf(input), added.queuedSubmission.input)
        call(fixture, CodexRpc.ThreadQueue.Reorder, ThreadQueueReorderParams(ThreadId("thr_1"), listOf("queue_1")),
            """{"threadId":"thr_1","queuedSubmissionIds":["queue_1"]}""", "{}")
        val removed = call(fixture, CodexRpc.ThreadQueue.Delete, ThreadQueueDeleteParams(ThreadId("thr_1"), "queue_1"),
            """{"threadId":"thr_1","queuedSubmissionId":"queue_1"}""", """{"deleted":false}""")
        assertFalse(removed.deleted)
    }

    @Test
    fun verificationCancellationUsesTheObservedOriginalRequestId() = runTest {
        val fixture = connect(experimental = true)
        val status = call(fixture, CodexRpc.UserVerification.Status, CodexRpcUnit, "{}",
            """{"unavailableReason":"credentialMissing"}""")
        assertEquals(UserVerificationUnavailableReason.CredentialMissing, status.unavailableReason)
        var observedId: CodexRequestId? = null
        val verify = async {
            fixture.first.request(CodexRpc.UserVerification.Verify, UserVerificationVerifyParams("AQ", "Approve", "Example")) { observedId = it }
        }
        runCurrent()
        val original = fixture.second.sent.last() as JsonRpcRequest
        assertEquals(CodexRequestId.Number((original.id as io.github.vupoint.cokit.protocol.JsonRpcId.Number).value), observedId)
        call(fixture, CodexRpc.UserVerification.Cancel, UserVerificationCancelParams(requireNotNull(observedId)),
            """{"requestId":${(original.id as io.github.vupoint.cokit.protocol.JsonRpcId.Number).value}}""", "{}")
        // A cancel acknowledgement does not resolve the original request or discard a late result.
        assertFalse(verify.isCompleted)
        verify.cancel()
        runCurrent()
        fixture.second.receive(JsonRpcResponse(original.id, CodexProtocolJson.parseToJsonElement("""{"proof":{"credentialId":"private-id","signature":"private-proof"}}""")))
        runCurrent()
        call(fixture, CodexRpc.UserVerification.Status, CodexRpcUnit, "{}", "{}")
        assertTrue(verify.isCancelled)
    }

    @Test
    fun verificationInputsValidateEncodingByteLimitsAndRedactProofs() {
        UserVerificationVerifyParams("AQ", "Approve", "")
        for (challenge in listOf("", "A", "AQ==", "AR", "a+", "a/", "A".repeat(5463))) {
            assertFailsWith<IllegalArgumentException> { UserVerificationVerifyParams(challenge, "Approve", "") }
        }
        assertFailsWith<IllegalArgumentException> { UserVerificationVerifyParams("AQ", "", "") }
        assertFailsWith<IllegalArgumentException> { UserVerificationVerifyParams("AQ", "é".repeat(129), "") }
        assertFailsWith<IllegalArgumentException> { UserVerificationVerifyParams("AQ", "Approve", "é".repeat(2049)) }
        assertFalse(UserVerificationVerifyParams("AQ", "private-title", "private-description").toString().contains("private"))
        assertFalse(UserVerificationVerifyResult(UserVerificationProof("private-id", "private-proof")).toString().contains("private"))
        assertEquals("\"external\"", CodexProtocolJson.encodeToString(CodexRequestId.serializer(), CodexRequestId.StringId("external")))
        assertFailsWith<kotlinx.serialization.SerializationException> {
            CodexProtocolJson.decodeFromString(CodexRequestId.serializer(), "true")
        }
    }

    private suspend fun TestScope.connect(experimental: Boolean = false): Pair<CodexClient, FakeJsonRpcTransport> {
        val transport = FakeJsonRpcTransport()
        val pending = async { CodexClients.connect(CodexClientConnection(
            transport, ClientInfo("cokit_test", "CoKit Test", "0.1.0"), backgroundScope,
            InitializeCapabilities(experimentalApi = experimental),
        )) }
        runCurrent()
        val initialize = transport.sent.single() as JsonRpcRequest
        transport.receive(JsonRpcResponse(initialize.id, CodexProtocolJson.parseToJsonElement("{}")))
        return pending.await() to transport
    }

    private suspend fun <P : Any, R : Any> TestScope.call(
        fixture: Pair<CodexClient, FakeJsonRpcTransport>, method: CodexRpcMethod<P, R>, params: P,
        expectedParams: String?, result: String,
    ): R {
        val pending = async { fixture.first.request(method, params) }
        runCurrent()
        val request = fixture.second.sent.last() as JsonRpcRequest
        assertEquals(method.method, request.method)
        assertEquals(expectedParams?.let(CodexProtocolJson::parseToJsonElement), request.params)
        fixture.second.receive(JsonRpcResponse(request.id, CodexProtocolJson.parseToJsonElement(result)))
        return pending.await()
    }
}
