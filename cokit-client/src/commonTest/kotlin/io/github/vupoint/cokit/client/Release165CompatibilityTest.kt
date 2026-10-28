package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.auth.AccountReadResult
import io.github.vupoint.cokit.client.auth.AccountPlanType
import io.github.vupoint.cokit.client.config.ConfigReadResult
import io.github.vupoint.cokit.client.extensions.HookEventName
import io.github.vupoint.cokit.client.policy.ManagedPolicyReadResult
import io.github.vupoint.cokit.client.policy.ManagedHooks
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlinx.serialization.SerializationException
import io.github.vupoint.cokit.client.mcp.McpServerStatusListResult
import io.github.vupoint.cokit.protocol.CodexProtocolJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

class Release165CompatibilityTest {
    @Test
    fun gcpAccountStateDecodesWithoutCredentials() {
        val payload = CodexProtocolJson.parseToJsonElement("""{"requiresOpenaiAuth":false,"account":{"type":"gcp"}}""")
        val result = CodexProtocolJson.decodeFromJsonElement<AccountReadResult>(payload)
        assertEquals(payload, CodexProtocolJson.encodeToJsonElement(result))
    }

    @Test
    fun mcpToolListMetadataPreservesUnknownNestedValues() {
        val payload = CodexProtocolJson.parseToJsonElement("""{"data":[{"name":"example","authStatus":"unsupported","toolListMeta":{"future":{"enabled":true},"revision":2}}]}""")
        val result = CodexProtocolJson.decodeFromJsonElement<McpServerStatusListResult>(payload)
        assertEquals(payload, CodexProtocolJson.encodeToJsonElement(result))
    }
    @Test
    fun newPlanAndHookNamesKeepTheirWireValues() {
        assertEquals("sci", AccountPlanType.Sci.value)
        assertEquals("sci_plus", AccountPlanType.SciPlus.value)
        assertEquals("preModelRequest", HookEventName.PreModelRequest.value)
        assertEquals("postModelRequest", HookEventName.PostModelRequest.value)
    }

    @Test
    fun opaqueImageErrorAndConfigurationChangesRemainReadable() {
        val turn = """{"id":"turn_1","status":"failed","items":[{"type":"imageGeneration","id":"image_1","status":"completed","fileId":"file_1","inputImages":[{"path":"/path/to/input.png","sha256":"example"}]}],"error":{"message":"Example","codexErrorInfo":"modelNotFound"}}"""
        val payload = CodexProtocolJson.parseToJsonElement(turn)
        assertEquals(payload, CodexProtocolJson.encodeToJsonElement(CodexProtocolJson.decodeFromJsonElement<Turn>(payload)))
        val config = CodexProtocolJson.parseToJsonElement("""{"config":{"application":{"storage":{"defaultProjectParent":"/path/to/projects","projectlessWorkspaceRoot":"/path/to/workspaces"}},"experimental_network":{"review_domains":["example.invalid"]},"hooks":{"PreModelRequest":[{"hooks":[],"matcher":null}],"PostModelRequest":[]}}}""")
        assertEquals(config, CodexProtocolJson.encodeToJsonElement(CodexProtocolJson.decodeFromJsonElement<ConfigReadResult>(config)))
    }

    @Test
    fun experimentalManagedStorageHooksAndReviewDomainsSurviveReadSnapshots() {
        val payload = CodexProtocolJson.parseToJsonElement("""{"requirements":{"application":{"storage":{"defaultProjectParent":"/path/to/projects","projectlessWorkspaceRoot":"/path/to/workspaces"}},"hooks":{"PreModelRequest":[{"hooks":[],"matcher":null}],"PostModelRequest":[]},"network":{"reviewDomains":["example.invalid"]}}}""")
        assertEquals(payload, CodexProtocolJson.encodeToJsonElement(CodexProtocolJson.decodeFromJsonElement<ManagedPolicyReadResult>(payload)))
    }

    @OptIn(ExperimentalCodexApi::class)
    @Test
    fun sensitiveNewPayloadsRedactDiagnosticsAndRejectWrongContainerTypes() {
        val privateText = "example-private-text"
        assertFalse(ThreadSampleParams(ThreadId("thr_1"), TurnId("turn_1"), privateText).toString().contains(privateText))
        assertFalse(ThreadSampleResult(TurnId("turn_1"), "response_1", privateText).toString().contains(privateText))
        assertFalse(ThreadMemoryReadResult(privateText).toString().contains(privateText))
        val hooks = ManagedHooks(mapOf("PreModelRequest" to listOf(CodexJsonPayload.parse("""{"command":"$privateText"}"""))))
        assertFalse(hooks.toString().contains(privateText))
        assertFalse(TurnMetadataPatch(mapOf("example" to privateText)).toString().contains(privateText))
        assertFalse(FeedbackSaveParams(privateText, CodexHostPath("/path/to/example.zip"), reason = privateText).toString().contains(privateText))
        assertFailsWith<SerializationException> {
            CodexProtocolJson.decodeFromString(ThreadSampleResult.serializer(), """{"sourceTurnId":[],"responseId":"response_1"}""")
        }
        assertFailsWith<SerializationException> {
            CodexProtocolJson.decodeFromString(ThreadMemoryReadResult.serializer(), """{"summary":{}}""")
        }
        assertFailsWith<SerializationException> {
            CodexProtocolJson.decodeFromString(TurnStartParams.serializer(), """{"threadId":"thr_1","input":[],"responsesapiClientMetadataPatch":[]}""")
        }
    }
}
