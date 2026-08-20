package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.commands.CommandNetworkAccess
import io.github.vupoint.cokit.protocol.CodexProtocolJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ThreadTurnModelTest {
    @Test
    fun stableThreadAndTurnParamsRoundTripAllReleaseFields() {
        assertJsonRoundTrip(
            """{"serviceTier":"priority","approvalPolicy":"never","approvalsReviewer":"auto_review","baseInstructions":"base","config":{"feature":true},"cwd":"/path/to/project","developerInstructions":"developer","serviceName":"desktop","sessionStartSource":"startup","ephemeral":true,"personality":"pragmatic","sandbox":"workspace-write","threadSource":"app","model":"gpt-5","modelProvider":"openai"}""",
            ThreadStartParams.serializer(),
        )
        assertJsonRoundTrip(
            """{"threadId":"thr_123","approvalPolicy":"on-request","approvalsReviewer":"user","baseInstructions":"base","config":{"feature":true},"cwd":"/path/to/project","developerInstructions":"developer","personality":"friendly","sandbox":"read-only","model":"gpt-5","modelProvider":"openai","serviceTier":"priority"}""",
            ThreadResumeParams.serializer(),
        )
        assertJsonRoundTrip(
            """{"threadId":"thr_123","approvalPolicy":"never","approvalsReviewer":"auto_review","baseInstructions":"base","config":{"feature":true},"cwd":"/path/to/project","sandbox":"workspace-write","developerInstructions":"developer","ephemeral":true,"threadSource":"app","lastTurnId":"turn_123","model":"gpt-5","modelProvider":"openai","serviceTier":"priority"}""",
            ThreadForkParams.serializer(),
        )
        assertJsonRoundTrip(
            """{"sourceKinds":["appServer","subAgent"],"archived":false,"cursor":"cursor_123","cwd":["/path/to/project","/path/to/other"],"isPinned":true,"limit":20,"modelProviders":["openai"],"useStateDbOnly":true,"searchTerm":"stable","sortDirection":"asc","sortKey":"updated_at"}""",
            ThreadListParams.serializer(),
        )
        assertJsonRoundTrip(
            """{"data":[{"id":"thr_123"}],"nextCursor":"next_123","backwardsCursor":"back_123"}""",
            ThreadListResult.serializer(),
        )
        assertJsonRoundTrip(
            """{"threadId":"thr_123","input":[],"approvalPolicy":"on-request","approvalsReviewer":"auto_review","clientUserMessageId":"msg_123","serviceTier":"priority","cwd":"/path/to/project","effort":"high","sandboxPolicy":{"type":"readOnly","networkAccess":false},"model":"gpt-5","summary":"concise","outputSchema":{"type":"object"},"personality":"pragmatic"}""",
            TurnStartParams.serializer(),
        )
    }

    @Test
    fun approvalPolicyRoundTripsGranularStableShape() {
        val fixture =
            """{"granular":{"mcp_elicitations":false,"request_permissions":true,"rules":false,"sandbox_approval":true,"skill_approval":true}}"""

        val decoded = runCatching {
            CodexProtocolJson.decodeFromString(ApprovalPolicy.serializer(), fixture)
        }

        assertTrue(decoded.isSuccess, "Granular stable approval policy should decode.")
        assertEquals(
            CodexJsonPayload.parse(fixture).toJsonElement(),
            CodexProtocolJson.encodeToJsonElement(ApprovalPolicy.serializer(), decoded.getOrThrow()),
        )
        val granular = assertIs<ApprovalPolicy.Granular>(decoded.getOrThrow()).granular
        assertEquals(true, granular.requestPermissions)
        assertEquals(true, granular.skillApproval)
    }

    @Test
    fun sandboxModeAndStructuredPoliciesUseDistinctWireShapes() {
        assertStringScalar(
            "workspace-write",
            SandboxMode.serializer(),
            SandboxMode.WorkspaceWrite,
        )

        val policies = listOf(
            SandboxPolicy.DangerFullAccess to "dangerFullAccess",
            SandboxPolicy.ReadOnly(networkAccess = false) to "readOnly",
            SandboxPolicy.ExternalSandbox(networkAccess = CommandNetworkAccess.Restricted) to "externalSandbox",
            SandboxPolicy.WorkspaceWrite(
                writableRoots = listOf(CodexHostPath("/path/to/project")),
                networkAccess = true,
            ) to "workspaceWrite",
        )

        policies.forEach { (policy, expectedType) ->
            val encoded = CodexProtocolJson.encodeToJsonElement(SandboxPolicy.serializer(), policy).jsonObject
            assertEquals(expectedType, encoded["type"]?.jsonPrimitive?.contentOrNull)
            assertEquals(
                policy,
                CodexProtocolJson.decodeFromJsonElement(SandboxPolicy.serializer(), encoded),
            )
        }
    }

    @Test
    fun serializesThreadTurnAndItemScalarModelsAsProtocolPrimitives() {
        assertStringScalar("item_123", ItemId.serializer(), ItemId("item_123"))
        assertStringScalar("cursor_123", CodexCursor.serializer(), CodexCursor("cursor_123"))
        assertStringScalar("msg_123", ClientMessageId.serializer(), ClientMessageId("msg_123"))
        assertStringScalar("notLoaded", ThreadStatusType.serializer(), ThreadStatusType.NotLoaded)
        assertStringScalar("inProgress", TurnStatus.serializer(), TurnStatus.InProgress)
        assertStringScalar("completed", ItemStatus.serializer(), ItemStatus.Completed)

        val encoded = CodexProtocolJson.encodeToString(
            CodexTimestamp.serializer(),
            CodexTimestamp(1730910000),
        )
        assertEquals("1730910000", encoded)
        assertEquals(
            CodexTimestamp(1730910000),
            CodexProtocolJson.decodeFromString(encoded),
        )
    }

    @Test
    fun threadModelsUseTypedScalarsWithoutChangingWireShape() {
        val encoded = CodexProtocolJson.encodeToJsonElement(
            Thread.serializer(),
            Thread(
                id = ThreadId("thr_123"),
                preview = "Fix tests",
                modelProvider = "openai",
                createdAt = CodexTimestamp(1730910000),
            ),
        ).jsonObject

        assertEquals("thr_123", encoded["id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("Fix tests", encoded["preview"]?.jsonPrimitive?.contentOrNull)
        assertEquals("openai", encoded["modelProvider"]?.jsonPrimitive?.contentOrNull)
        assertEquals(
            1730910000,
            CodexProtocolJson.decodeFromJsonElement<Int>(encoded.getValue("createdAt")),
        )

        val listParams = CodexProtocolJson.encodeToJsonElement(
            ThreadListParams.serializer(),
            ThreadListParams(cursor = CodexCursor("cursor_123")),
        ).jsonObject

        assertEquals("cursor_123", listParams["cursor"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun turnModelsUseTypedScalarsWithoutChangingWireShape() {
        val turn = CodexProtocolJson.decodeFromString<Turn>(
            """{"id":"turn_123","status":"inProgress","items":[]}""",
        )

        assertEquals(TurnId("turn_123"), turn.id)
        assertEquals(TurnStatus.InProgress, turn.status)

        val steerParams = CodexProtocolJson.encodeToJsonElement(
            TurnSteerParams.serializer(),
            TurnSteerParams(
                threadId = ThreadId("thr_123"),
                expectedTurnId = TurnId("turn_123"),
                input = listOf(TurnInput.Text("Keep going")),
                clientUserMessageId = ClientMessageId("msg_123"),
            ),
        ).jsonObject

        assertEquals("msg_123", steerParams["clientUserMessageId"]?.jsonPrimitive?.contentOrNull)
    }

    private fun <T> assertStringScalar(
        expected: String,
        serializer: KSerializer<T>,
        value: T,
    ) {
        val encoded = CodexProtocolJson.encodeToString(serializer, value)

        assertEquals(CodexProtocolJson.encodeToString(String.serializer(), expected), encoded)
        assertEquals(value, CodexProtocolJson.decodeFromString(serializer, encoded))
    }

    private fun <T> assertJsonRoundTrip(
        fixture: String,
        serializer: KSerializer<T>,
    ) {
        val decoded = runCatching {
            CodexProtocolJson.decodeFromString(serializer, fixture)
        }
        assertTrue(decoded.isSuccess, "Stable fixture should decode: $fixture")
        assertEquals(
            CodexJsonPayload.parse(fixture).toJsonElement(),
            CodexProtocolJson.encodeToJsonElement(serializer, decoded.getOrThrow()),
        )
    }
}
