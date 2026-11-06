package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.auth.AccountReadResult
import io.github.vupoint.cokit.client.auth.AccountSessionListParams
import io.github.vupoint.cokit.client.auth.AccountSessionListResult
import io.github.vupoint.cokit.client.auth.AccountSessionSwitchParams
import io.github.vupoint.cokit.client.auth.AccountSessionSwitchResult
import io.github.vupoint.cokit.client.auth.GcpSetupParams
import io.github.vupoint.cokit.client.extensions.HooksListParams
import io.github.vupoint.cokit.client.extensions.HooksListResult
import io.github.vupoint.cokit.client.policy.ManagedPolicyReadResult
import io.github.vupoint.cokit.client.review.ReviewStartParams
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
class Release167SchemaFixturesTest {
    @Test
    fun schemaValidatedFixturesRoundTripThroughPublicContracts() {
        val resource = javaClass.classLoader.getResourceAsStream("fixtures/release-167-contracts.json")
        assertNotNull(resource)
        val fixtures = resource.bufferedReader().use { CodexProtocolJson.parseToJsonElement(it.readText()).jsonArray }
        for (fixture in fixtures) {
            val entry = fixture.jsonObject
            val schema = entry.getValue("schema").jsonPrimitive.content
            val serializer = when (schema) {
                "GetAccountResponse" -> AccountReadResult.serializer()
                "HooksListParams" -> HooksListParams.serializer()
                "HooksListResponse" -> HooksListResult.serializer()
                "ThreadSectionChangedNotification" -> CodexNotification.ThreadSectionChanged.serializer()
                "ThreadItem" -> ThreadItemSummary.serializer()
                "ConfigRequirementsReadResponse" -> ManagedPolicyReadResult.serializer()
                "ReviewStartParams" -> ReviewStartParams.serializer()
                "AccountSessionsListParams" -> AccountSessionListParams.serializer()
                "AccountSessionsResponse" -> AccountSessionListResult.serializer()
                "AccountSessionsSwitchParams" -> AccountSessionSwitchParams.serializer()
                "AccountSessionsSwitchResponse" -> AccountSessionSwitchResult.serializer()
                "GcpSetupParams" -> GcpSetupParams.serializer()
                "ThreadGuardianReviewParams" -> ThreadGuardianReviewParams.serializer()
                "ThreadGuardianReviewResponse" -> ThreadGuardianReviewResult.serializer()
                "ThreadSampleParams" -> ThreadSampleParams.serializer()
                else -> error("Unmapped schema fixture: $schema")
            }
            val payload = entry.getValue("payload")
            roundTrip(serializer, payload, entry["encoded"] ?: payload, schema)
        }
    }

    private fun <T> roundTrip(serializer: KSerializer<T>, payload: JsonElement, expected: JsonElement, schema: String) {
        val value = CodexProtocolJson.decodeFromJsonElement(serializer, payload)
        assertEquals(expected, CodexProtocolJson.encodeToJsonElement(serializer, value), schema)
    }
}
