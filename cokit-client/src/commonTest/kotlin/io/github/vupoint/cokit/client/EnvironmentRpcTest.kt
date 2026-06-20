package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.environment.CollaborationModeKind
import io.github.vupoint.cokit.client.environment.CollaborationModeListParams
import io.github.vupoint.cokit.client.environment.EnvironmentAddParams
import io.github.vupoint.cokit.client.environment.EnvironmentId
import io.github.vupoint.cokit.client.environment.PermissionProfileId
import io.github.vupoint.cokit.client.environment.PermissionProfileListParams
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@OptIn(ExperimentalCodexApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EnvironmentRpcTest {
    @Test
    fun permissionProfileListDescriptorSendsPagingParamsAndDecodesProfiles() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val result = async {
            fixture.client.request(
                CodexRpc.PermissionProfile.List,
                PermissionProfileListParams(
                    cursor = CodexCursor("cursor_permissions"),
                    cwd = CodexHostPath("/path/to/project"),
                    limit = 25,
                ),
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("permissionProfile/list", request.method)
        val params = request.params!!.jsonObject
        assertEquals("cursor_permissions", params["cursor"]?.jsonPrimitive?.contentOrNull)
        assertEquals("/path/to/project", params["cwd"]?.jsonPrimitive?.contentOrNull)
        assertEquals("25", params["limit"]?.jsonPrimitive?.contentOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put(
                        "data",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("id", "workspace-write")
                                    put("description", "Workspace write access")
                                },
                            )
                        },
                    )
                    put("nextCursor", "cursor_next")
                },
            ),
        )

        val decoded = result.await()
        assertEquals(CodexCursor("cursor_next"), decoded.nextCursor)
        assertEquals(PermissionProfileId("workspace-write"), decoded.data.single().id)
        assertEquals("Workspace write access", decoded.data.single().description)
    }

    @Test
    fun experimentalEnvironmentDescriptorsUseCurrentSchemaMethods() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val modesResult = async {
            fixture.client.request(
                CodexRpc.CollaborationMode.List,
                CollaborationModeListParams,
            )
        }
        runCurrent()

        val modes = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("collaborationMode/list", modes.method)
        assertEquals(emptyMap(), modes.params!!.jsonObject)
        fixture.transport.receive(
            JsonRpcResponse(
                modes.id,
                result = buildJsonObject {
                    put(
                        "data",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("name", "Planning")
                                    put("mode", "plan")
                                    put("model", "gpt-5")
                                    put("reasoning_effort", "high")
                                },
                            )
                        },
                    )
                },
            ),
        )

        val mode = modesResult.await().data.single()
        assertEquals("Planning", mode.name)
        assertEquals(CollaborationModeKind.Plan, mode.mode)
        assertEquals(ModelName("gpt-5"), mode.model)
        assertEquals(ReasoningEffort.High, mode.reasoningEffort)

        val addResult = async {
            fixture.client.request(
                CodexRpc.Environment.Add,
                EnvironmentAddParams(
                    environmentId = EnvironmentId("env_remote_1"),
                    execServerUrl = "https://exec.example.invalid",
                ),
            )
        }
        runCurrent()

        val add = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("environment/add", add.method)
        val addParams = add.params!!.jsonObject
        assertEquals("env_remote_1", addParams["environmentId"]?.jsonPrimitive?.contentOrNull)
        assertEquals("https://exec.example.invalid", addParams["execServerUrl"]?.jsonPrimitive?.contentOrNull)
        fixture.transport.receive(JsonRpcResponse(add.id, result = JsonObject(emptyMap())))
        assertEquals(CodexRpcUnit, addResult.await())
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
