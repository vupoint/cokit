package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.skills.SkillConfigWriteParams
import io.github.vupoint.cokit.client.skills.SkillDependencies
import io.github.vupoint.cokit.client.skills.SkillName
import io.github.vupoint.cokit.client.skills.SkillScope
import io.github.vupoint.cokit.client.skills.SkillToolDependency
import io.github.vupoint.cokit.client.skills.SkillToolDependencyType
import io.github.vupoint.cokit.client.skills.SkillsExtraRootsSetParams
import io.github.vupoint.cokit.client.skills.SkillsListParams
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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SkillRpcTest {
    @Test
    fun skillsListDescriptorSendsScopeAndDecodesSkillMetadata() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val result = async {
            fixture.client.request(
                CodexRpc.Skills.List,
                SkillsListParams(
                    cwds = listOf(CodexHostPath("/path/to/project")),
                    forceReload = true,
                ),
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("skills/list", request.method)
        val params = request.params!!.jsonObject
        assertEquals("/path/to/project", params["cwds"]!!.jsonArray.single().jsonPrimitive.contentOrNull)
        assertEquals(true, params["forceReload"]?.jsonPrimitive?.booleanOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put(
                        "data",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("cwd", "/path/to/project")
                                    put(
                                        "errors",
                                        buildJsonArray {
                                            add(
                                                buildJsonObject {
                                                    put("message", "Invalid skill metadata")
                                                    put("path", "/path/to/project/.codex/skills/broken/SKILL.md")
                                                },
                                            )
                                        },
                                    )
                                    put(
                                        "skills",
                                        buildJsonArray {
                                            add(
                                                buildJsonObject {
                                                    put("name", "review-helper")
                                                    put("description", "Review code changes")
                                                    put("enabled", true)
                                                    put("path", "/path/to/project/.codex/skills/review-helper")
                                                    put("scope", "repo")
                                                    put("shortDescription", "Review changes")
                                                    put(
                                                        "interface",
                                                        buildJsonObject {
                                                            put("displayName", "Review Helper")
                                                            put("shortDescription", "Review changes")
                                                            put("brandColor", "#3366ff")
                                                            put("iconSmall", "/path/to/project/.codex/skills/review-helper/icon-small.png")
                                                        },
                                                    )
                                                    put(
                                                        "dependencies",
                                                        buildJsonObject {
                                                            put(
                                                                "tools",
                                                                buildJsonArray {
                                                                    add(
                                                                        buildJsonObject {
                                                                            put("type", "mcp")
                                                                            put("value", "github")
                                                                            put("transport", "stdio")
                                                                            put("description", "GitHub context")
                                                                        },
                                                                    )
                                                                },
                                                            )
                                                        },
                                                    )
                                                },
                                            )
                                        },
                                    )
                                },
                            )
                        },
                    )
                },
            ),
        )

        val entry = result.await().data.single()
        assertEquals(CodexHostPath("/path/to/project"), entry.cwd)
        assertEquals("Invalid skill metadata", entry.errors.single().message)
        assertEquals(CodexHostPath("/path/to/project/.codex/skills/broken/SKILL.md"), entry.errors.single().path)

        val skill = entry.skills.single()
        assertEquals(SkillName("review-helper"), skill.name)
        assertEquals("Review code changes", skill.description)
        assertEquals(true, skill.enabled)
        assertEquals(CodexHostPath("/path/to/project/.codex/skills/review-helper"), skill.path)
        assertEquals(SkillScope.Repo, skill.scope)
        assertEquals("Review Helper", skill.interfaceMetadata?.displayName)
        assertEquals(CodexHostPath("/path/to/project/.codex/skills/review-helper/icon-small.png"), skill.interfaceMetadata?.iconSmall)
        assertEquals(
            SkillDependencies(
                tools = listOf(
                    SkillToolDependency(
                        type = SkillToolDependencyType.Mcp,
                        value = "github",
                        transport = "stdio",
                        description = "GitHub context",
                    ),
                ),
            ),
            skill.dependencies,
        )
    }

    @Test
    fun skillsConfigAndExtraRootsDescriptorsUseCurrentSchemaMethods() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val extraRootsResult = async {
            fixture.client.request(
                CodexRpc.Skills.SetExtraRoots,
                SkillsExtraRootsSetParams(
                    extraRoots = listOf(CodexHostPath("/path/to/shared-skills")),
                ),
            )
        }
        runCurrent()

        val extraRoots = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("skills/extraRoots/set", extraRoots.method)
        assertEquals(
            "/path/to/shared-skills",
            extraRoots.params!!.jsonObject["extraRoots"]!!.jsonArray.single().jsonPrimitive.contentOrNull,
        )
        fixture.transport.receive(JsonRpcResponse(extraRoots.id, result = JsonObject(emptyMap())))
        assertEquals(CodexRpcUnit, extraRootsResult.await())

        val writeConfigResult = async {
            fixture.client.request(
                CodexRpc.Skills.WriteConfig,
                SkillConfigWriteParams(
                    name = SkillName("review-helper"),
                    path = CodexHostPath("/path/to/project/.codex/skills/review-helper"),
                    enabled = false,
                ),
            )
        }
        runCurrent()

        val writeConfig = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("skills/config/write", writeConfig.method)
        val writeParams = writeConfig.params!!.jsonObject
        assertEquals("review-helper", writeParams["name"]?.jsonPrimitive?.contentOrNull)
        assertEquals("/path/to/project/.codex/skills/review-helper", writeParams["path"]?.jsonPrimitive?.contentOrNull)
        assertEquals(false, writeParams["enabled"]?.jsonPrimitive?.booleanOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                writeConfig.id,
                result = buildJsonObject {
                    put("effectiveEnabled", false)
                },
            ),
        )
        assertEquals(false, writeConfigResult.await().effectiveEnabled)
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
