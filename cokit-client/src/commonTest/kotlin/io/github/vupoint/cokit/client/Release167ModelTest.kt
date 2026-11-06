package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.auth.AccountSession
import io.github.vupoint.cokit.client.auth.AccountSessionId
import io.github.vupoint.cokit.client.auth.AccountSessionSwitchResult
import io.github.vupoint.cokit.client.auth.AccountSessionWorkspace
import io.github.vupoint.cokit.client.auth.AccountWorkspaceId
import io.github.vupoint.cokit.client.auth.GcpSetupParams
import io.github.vupoint.cokit.client.policy.ManagedPolicyReadResult
import io.github.vupoint.cokit.protocol.CodexProtocolJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@OptIn(ExperimentalCodexApi::class)
class Release167ModelTest {
    @Test
    fun unknownSessionResultsAndGuardianCategoriesRemainReadableWithoutGrantingAccess() {
        val payload = json("""{"futureOutcome":{"private":"example"}}""")
        val result = CodexProtocolJson.decodeFromJsonElement<AccountSessionSwitchResult>(payload)
        assertIs<AccountSessionSwitchResult.Unknown>(result)
        assertEquals(payload, CodexProtocolJson.encodeToJsonElement(AccountSessionSwitchResult.serializer(), result))
        assertFalse(result.toString().contains("example"))
        val guardian = json("""{"callId":"call_1","assessment":{"outcome":"future","riskLevel":"future","userAuthorization":"future","rationale":"private reason"}}""")
        assertEquals(guardian, CodexProtocolJson.encodeToJsonElement(CodexProtocolJson.decodeFromJsonElement<ThreadGuardianReviewResult>(guardian)))
    }

    @Test
    fun malformedSessionAndPolicyContainersFailDecoding() {
        for (payload in listOf("[]", """{"sessions":{}}""", """{"loginId":"login_1"}""", """{"login_id":[],"auth_url":"https://example.invalid"}""")) {
            assertFailsWith<SerializationException> {
                CodexProtocolJson.decodeFromString(AccountSessionSwitchResult.serializer(), payload)
            }
        }
        assertFailsWith<SerializationException> {
            CodexProtocolJson.decodeFromString(ManagedPolicyReadResult.serializer(), """{"requirements":{"otel":[]}}""")
        }
        assertFailsWith<SerializationException> {
            CodexProtocolJson.decodeFromString(ThreadGuardianReviewResult.serializer(), """{"callId":"call_1","assessment":{}}""")
        }
    }

    @Test
    fun accountSessionAndGcpDiagnosticsRedactIdentitiesAndHostPaths() {
        val privateValue = "example-private-value"
        val workspace = AccountSessionWorkspace(AccountWorkspaceId(privateValue), name = privateValue, imageUrl = privateValue)
        val session = AccountSession(AccountSessionId(privateValue), true, 0, listOf(workspace), userId = privateValue, displayName = privateValue)
        for (value in listOf(workspace.accountId, session.sessionId, workspace, session, GcpSetupParams(privateValue, privateValue, CodexHostPath(privateValue)))) {
            assertFalse(value.toString().contains(privateValue))
        }
    }

    @Test
    fun audioFileReferencesAndImageGenerationFieldsSurviveOpaquePayloads() {
        for (audio in listOf("""{"type":"audio","url":"https://example.invalid/audio"}""", """{"type":"audio","fileId":"file_1"}""")) {
            val payload = json(audio)
            assertEquals(payload, CodexProtocolJson.encodeToJsonElement(CodexProtocolJson.decodeFromJsonElement<TurnInput>(payload)))
        }
        val turn = json("""{"id":"turn_1","status":"failed","items":[{"type":"imageGeneration","id":"item_1","status":"completed","generationId":"generation_1","fileId":"file_1","startedAtMs":1,"completedAtMs":2}],"error":{"message":"Example","codexErrorInfo":"policyCapacityUnavailable"}}""")
        assertEquals(turn, CodexProtocolJson.encodeToJsonElement(CodexProtocolJson.decodeFromJsonElement<Turn>(turn)))
    }

    private fun json(value: String) = CodexProtocolJson.parseToJsonElement(value)
}
