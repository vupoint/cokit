package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.models.InputModality
import io.github.vupoint.cokit.client.models.ModelCatalogId
import io.github.vupoint.cokit.client.models.ModelListParams
import io.github.vupoint.cokit.client.models.ModelProviderCapabilities
import io.github.vupoint.cokit.client.models.ModelProviderCapabilitiesReadParams
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ModelCatalogRpcTest {
    @Test
    fun modelListDescriptorSendsPagingParamsAndDecodesCatalogPage() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val result = async {
            fixture.client.request(
                CodexRpc.Model.List,
                ModelListParams(
                    cursor = CodexCursor("cursor_models"),
                    includeHidden = true,
                    limit = 2,
                ),
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("model/list", request.method)
        val params = request.params!!.jsonObject
        assertEquals("cursor_models", params["cursor"]?.jsonPrimitive?.contentOrNull)
        assertEquals(true, params["includeHidden"]?.jsonPrimitive?.booleanOrNull)
        assertEquals("2", params["limit"]?.jsonPrimitive?.contentOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put(
                        "data",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("id", "gpt-5")
                                    put("model", "gpt-5")
                                    put("displayName", "GPT-5")
                                    put("description", "General coding and reasoning model")
                                    put("hidden", false)
                                    put("isDefault", true)
                                    put("defaultReasoningEffort", "medium")
                                    put(
                                        "supportedReasoningEfforts",
                                        buildJsonArray {
                                            add(
                                                buildJsonObject {
                                                    put("reasoningEffort", "low")
                                                    put("description", "Lower latency")
                                                },
                                            )
                                        },
                                    )
                                    put("inputModalities", buildJsonArray {
                                        add("text")
                                        add("image")
                                    })
                                    put("supportsPersonality", true)
                                    put("defaultServiceTier", "priority")
                                    put(
                                        "serviceTiers",
                                        buildJsonArray {
                                            add(
                                                buildJsonObject {
                                                    put("id", "priority")
                                                    put("name", "Priority")
                                                    put("description", "Lower latency routing")
                                                },
                                            )
                                        },
                                    )
                                    put("additionalSpeedTiers", buildJsonArray {
                                        add("auto")
                                    })
                                    put("availabilityNux", buildJsonObject {
                                        put("message", "Available to this account")
                                    })
                                    put("upgrade", JsonNull)
                                    put(
                                        "upgradeInfo",
                                        buildJsonObject {
                                            put("model", "gpt-5.1")
                                            put("modelLink", "https://example.invalid/models/gpt-5.1")
                                            put("migrationMarkdown", "Use gpt-5.1 for new work.")
                                            put("upgradeCopy", "Try GPT-5.1")
                                        },
                                    )
                                },
                            )
                        },
                    )
                    put("nextCursor", "cursor_next")
                },
            ),
        )

        val page = result.await()
        assertEquals(CodexCursor("cursor_next"), page.nextCursor)
        val model = page.data.single()
        assertEquals(ModelCatalogId("gpt-5"), model.id)
        assertEquals(ModelName("gpt-5"), model.model)
        assertEquals("GPT-5", model.displayName)
        assertEquals("General coding and reasoning model", model.description)
        assertFalse(model.hidden)
        assertTrue(model.isDefault)
        assertEquals(ReasoningEffort.Medium, model.defaultReasoningEffort)
        assertEquals(ReasoningEffort.Low, model.supportedReasoningEfforts.single().reasoningEffort)
        assertEquals("Lower latency", model.supportedReasoningEfforts.single().description)
        assertEquals(listOf(InputModality.Text, InputModality.Image), model.inputModalities)
        assertTrue(model.supportsPersonality)
        assertEquals("priority", model.defaultServiceTier)
        assertEquals("Priority", model.serviceTiers.single().name)
        assertEquals("auto", model.additionalSpeedTiers.single())
        assertEquals("Available to this account", model.availabilityNux?.message)
        assertEquals("gpt-5.1", model.upgradeInfo?.model?.value)
    }

    @Test
    fun modelProviderCapabilitiesDescriptorSendsEmptyParamsAndDecodesFlags() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val result = async {
            fixture.client.request(
                CodexRpc.Model.ReadProviderCapabilities,
                ModelProviderCapabilitiesReadParams,
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("modelProvider/capabilities/read", request.method)
        assertEquals(emptyMap(), request.params!!.jsonObject)

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put("webSearch", true)
                    put("imageGeneration", false)
                    put("namespaceTools", true)
                },
            ),
        )

        assertEquals(
            ModelProviderCapabilities(
                webSearch = true,
                imageGeneration = false,
                namespaceTools = true,
            ),
            result.await(),
        )
    }

    private suspend fun TestScope.connectedRpcClientFixture(
        scope: CoroutineScope,
    ): ConnectedRpcClientFixture {
        val transport = FakeJsonRpcTransport()
        val client = async {
            CodexRpcClient.connect(
                CodexRpcConnection(
                    transport = transport,
                    clientInfo = ClientInfo("cokit_test", "CoKit Test", "0.1.0"),
                    scope = scope,
                ),
            )
        }
        runCurrent()
        val initialize = transport.sent.single() as JsonRpcRequest
        transport.receive(JsonRpcResponse(initialize.id, result = JsonObject(emptyMap())))
        return ConnectedRpcClientFixture(client.await(), transport)
    }

    private data class ConnectedRpcClientFixture(
        val client: CodexRpcClient,
        val transport: FakeJsonRpcTransport,
    )
}
