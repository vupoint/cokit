package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.auth.BedrockGovCloudRequirementsResult
import io.github.vupoint.cokit.client.environment.EnvironmentAddParams
import io.github.vupoint.cokit.client.mcp.McpServerOauthLoginResult
import io.github.vupoint.cokit.client.models.ModelProviderCapabilities
import io.github.vupoint.cokit.client.policy.ManagedPolicyReadResult
import io.github.vupoint.cokit.protocol.CodexProtocolJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalCodexApi::class)
class Release163SchemaFixturesTest {
    @Test
    fun schemaValidatedFixturesRoundTripThroughPublicContracts() {
        val resource = javaClass.classLoader.getResourceAsStream("fixtures/release-163-contracts.json")
        assertNotNull(resource)
        val fixtures = resource.bufferedReader().use { CodexProtocolJson.parseToJsonElement(it.readText()).jsonArray }
        for (fixture in fixtures) {
            val schema = fixture.jsonObject.getValue("schema").jsonPrimitive.content
            val serializer = when (schema) {
                "ModelProviderCapabilitiesReadResponse" -> ModelProviderCapabilities.serializer()
                "ThreadListParams" -> ThreadListParams.serializer()
                "ThreadGoalSetParams" -> ThreadGoalSetParams.serializer()
                "ThreadGoalClearParams" -> ThreadGoalClearParams.serializer()
                "TurnStartParams" -> TurnStartParams.serializer()
                "McpServerOauthLoginResponse" -> McpServerOauthLoginResult.serializer()
                "McpServerOauthLoginCompletedNotification" -> CodexNotification.McpServerOauthLoginCompleted.serializer()
                "ConfigRequirementsReadResponse" -> ManagedPolicyReadResult.serializer()
                "ThreadAttachmentOwnerListParams" -> ThreadAttachmentOwnerListParams.serializer()
                "ThreadAttachmentOwnerListResponse" -> ThreadAttachmentOwnerListResult.serializer()
                "ThreadItemsReadParams" -> ThreadItemsReadParams.serializer()
                "ThreadItemsReadResponse" -> ThreadItemsReadResult.serializer()
                "ThreadReadStateUpdateParams" -> ThreadReadStateUpdateParams.serializer()
                "ThreadReadStateUpdateResponse" -> ThreadReadStateUpdateResult.serializer()
                "ThreadReadStateChangedNotification" -> CodexNotification.ThreadReadStateChanged.serializer()
                "ThreadPredictionRequestParams" -> ThreadPredictionRequestParams.serializer()
                "ThreadPredictionUpdatedNotification" -> CodexNotification.ThreadPredictionUpdated.serializer()
                "BedrockCheckGovCloudRequirementsResponse" -> BedrockGovCloudRequirementsResult.serializer()
                "EnvironmentAddParams" -> EnvironmentAddParams.serializer()
                "ThreadForkParams" -> ThreadForkParams.serializer()
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
