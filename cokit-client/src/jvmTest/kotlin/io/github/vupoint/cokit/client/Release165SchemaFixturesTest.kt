package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.auth.AccountReadResult
import io.github.vupoint.cokit.client.policy.ManagedPolicyReadResult
import io.github.vupoint.cokit.client.mcp.McpServerStatusListResult
import io.github.vupoint.cokit.protocol.CodexProtocolJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalCodexApi::class)
class Release165SchemaFixturesTest {
    @Test
    fun schemaValidatedFixturesRoundTripThroughPublicContracts() {
        val resource = javaClass.classLoader.getResourceAsStream("fixtures/release-165-contracts.json")
        assertNotNull(resource)
        val fixtures = resource.bufferedReader().use { CodexProtocolJson.parseToJsonElement(it.readText()).jsonArray }
        for (fixture in fixtures) {
            val schema = fixture.jsonObject.getValue("schema").jsonPrimitive.content
            val serializer = when (schema) {
                "ConfigRequirementsReadResponse" -> ManagedPolicyReadResult.serializer()
                "FeedbackSaveParams" -> FeedbackSaveParams.serializer()
                "FeedbackSaveResponse" -> FeedbackSaveResult.serializer()
                "GetAccountResponse" -> AccountReadResult.serializer()
                "ListMcpServerStatusResponse" -> McpServerStatusListResult.serializer()
                "ThreadSampleParams" -> ThreadSampleParams.serializer()
                "ThreadSampleResponse" -> ThreadSampleResult.serializer()
                "ThreadSampleCancelParams" -> ThreadSampleCancelParams.serializer()
                "ThreadMemoryReadParams" -> ThreadMemoryReadParams.serializer()
                "ThreadMemoryReadResponse" -> ThreadMemoryReadResult.serializer()
                "ThreadForkParams" -> ThreadForkParams.serializer()
                "ThreadListParams" -> ThreadListParams.serializer()
                "TurnStartParams" -> TurnStartParams.serializer()
                "TurnSteerParams" -> TurnSteerParams.serializer()
                else -> error("Unmapped schema fixture: $schema")
            }
            roundTrip(serializer, fixture.jsonObject.getValue("payload"), schema)
        }
    }

    private fun <T> roundTrip(serializer: KSerializer<T>, payload: JsonElement, schema: String) {
        val value = CodexProtocolJson.decodeFromJsonElement(serializer, payload)
        assertEquals(payload, CodexProtocolJson.encodeToJsonElement(serializer, value), schema)
    }
}
