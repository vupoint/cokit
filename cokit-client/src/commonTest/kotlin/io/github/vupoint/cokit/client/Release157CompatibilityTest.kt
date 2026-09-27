package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.approvals.CommandApprovalRequest
import io.github.vupoint.cokit.client.mcp.McpElicitationRequest
import io.github.vupoint.cokit.protocol.CodexProtocolJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

class Release157CompatibilityTest {
    @Test
    fun commandApprovalPreservesStdinAndUnknownKinds() {
        for (kind in listOf("writeStdin", "futureAction")) {
            roundTrip(
                """{"threadId":"thr_1","turnId":"turn_1","itemId":"item_1","startedAtMs":1,"kind":"$kind"}""",
                CommandApprovalRequest.serializer(),
            )
        }
    }

    @Test
    fun initializePreservesExtensionDeclarations() {
        roundTrip(
            """{"extensions":{"openai/form":{}},"explicitGatewayOauth":true}""",
            InitializeCapabilities.serializer(),
        )
    }

    @Test
    fun openAiFormAliasesDecodeTheSameTypedRequest() {
        for (mode in listOf("openai/form", "openaiForm")) {
            val request = CodexProtocolJson.decodeFromJsonElement(
                McpElicitationRequest.serializer(),
                CodexProtocolJson.parseToJsonElement(
                    """{"serverName":"example","threadId":"thr_1","message":"Confirm","mode":"$mode","requestedSchema":{"type":"object"}}""",
                ),
            )
            assertIs<McpElicitationRequest.OpenAiForm>(request)
        }
    }

    private fun <T> roundTrip(fixture: String, serializer: KSerializer<T>) {
        val expected = CodexProtocolJson.parseToJsonElement(fixture)
        val decoded = CodexProtocolJson.decodeFromJsonElement(serializer, expected)
        assertEquals(expected, CodexProtocolJson.encodeToJsonElement(serializer, decoded))
    }
}
