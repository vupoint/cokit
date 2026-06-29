package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.mcp.McpAuthStatus
import io.github.vupoint.cokit.client.mcp.McpConfigReloadParams
import io.github.vupoint.cokit.client.mcp.McpResourceContent
import io.github.vupoint.cokit.client.mcp.McpResourceReadParams
import io.github.vupoint.cokit.client.mcp.McpResourceUri
import io.github.vupoint.cokit.client.mcp.McpServerName
import io.github.vupoint.cokit.client.mcp.McpServerOauthLoginParams
import io.github.vupoint.cokit.client.mcp.McpServerStatusDetail
import io.github.vupoint.cokit.client.mcp.McpServerStatusListParams
import io.github.vupoint.cokit.client.mcp.McpServerToolCallParams
import io.github.vupoint.cokit.client.mcp.McpToolName
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
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
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class McpRpcTest {
    @Test
    fun mcpStatusDescriptorSendsPagingParamsAndDecodesServerCatalog() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val result = async {
            fixture.client.request(
                CodexRpc.Mcp.ListServerStatus,
                McpServerStatusListParams(
                    cursor = CodexCursor("cursor_mcp"),
                    detail = McpServerStatusDetail.Full,
                    limit = 20,
                    threadId = ThreadId("thread_123"),
                ),
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("mcpServerStatus/list", request.method)
        val params = request.params!!.jsonObject
        assertEquals("cursor_mcp", params["cursor"]?.jsonPrimitive?.contentOrNull)
        assertEquals("full", params["detail"]?.jsonPrimitive?.contentOrNull)
        assertEquals(20, params["limit"]?.jsonPrimitive?.intOrNull)
        assertEquals("thread_123", params["threadId"]?.jsonPrimitive?.contentOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put(
                        "data",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("name", "github")
                                    put("authStatus", "oAuth")
                                    put(
                                        "serverInfo",
                                        buildJsonObject {
                                            put("name", "GitHub MCP")
                                            put("title", "GitHub")
                                            put("version", "1.2.3")
                                            put("description", "Repository tools")
                                            put("websiteUrl", "https://mcp.example.invalid/github")
                                            put(
                                                "icons",
                                                buildJsonArray {
                                                    add(
                                                        buildJsonObject {
                                                            put("src", "https://mcp.example.invalid/github/icon.png")
                                                        },
                                                    )
                                                },
                                            )
                                        },
                                    )
                                    put(
                                        "resources",
                                        buildJsonArray {
                                            add(
                                                buildJsonObject {
                                                    put("uri", "github://repos/openai/codex")
                                                    put("name", "codex")
                                                    put("title", "Codex Repository")
                                                    put("description", "Repository metadata")
                                                    put("mimeType", "application/json")
                                                    put("size", 42)
                                                    put(
                                                        "_meta",
                                                        buildJsonObject {
                                                            put("source", "fixture")
                                                        },
                                                    )
                                                },
                                            )
                                        },
                                    )
                                    put(
                                        "resourceTemplates",
                                        buildJsonArray {
                                            add(
                                                buildJsonObject {
                                                    put("uriTemplate", "github://repos/{owner}/{repo}")
                                                    put("name", "repo")
                                                    put("title", "Repository")
                                                    put("description", "Repository by owner/name")
                                                    put("mimeType", "application/json")
                                                },
                                            )
                                        },
                                    )
                                    put(
                                        "tools",
                                        buildJsonObject {
                                            put(
                                                "search_issues",
                                                buildJsonObject {
                                                    put("name", "search_issues")
                                                    put("title", "Search Issues")
                                                    put("description", "Search repository issues")
                                                    put(
                                                        "inputSchema",
                                                        buildJsonObject {
                                                            put("type", "object")
                                                            put(
                                                                "properties",
                                                                buildJsonObject {
                                                                    put(
                                                                        "query",
                                                                        buildJsonObject {
                                                                            put("type", "string")
                                                                        },
                                                                    )
                                                                },
                                                            )
                                                        },
                                                    )
                                                    put(
                                                        "_meta",
                                                        buildJsonObject {
                                                            put("safe", true)
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
                    put("nextCursor", "cursor_next")
                },
            ),
        )

        val decoded = result.await()
        assertEquals(CodexCursor("cursor_next"), decoded.nextCursor)

        val server = decoded.data.single()
        assertEquals(McpServerName("github"), server.name)
        assertEquals(McpAuthStatus.OAuth, server.authStatus)
        assertEquals("GitHub", server.serverInfo?.title)
        assertEquals("1.2.3", server.serverInfo?.version)
        assertEquals("""[{"src":"https://mcp.example.invalid/github/icon.png"}]""", server.serverInfo?.icons?.toJsonString())

        val resource = server.resources.single()
        assertEquals(McpResourceUri("github://repos/openai/codex"), resource.uri)
        assertEquals("Codex Repository", resource.title)
        assertEquals("""{"source":"fixture"}""", resource.meta?.toJsonString())

        val template = server.resourceTemplates.single()
        assertEquals("github://repos/{owner}/{repo}", template.uriTemplate)
        assertEquals("Repository", template.title)

        val tool = server.tools.getValue(McpToolName("search_issues"))
        assertEquals("Search Issues", tool.title)
        assertEquals("""{"type":"object","properties":{"query":{"type":"string"}}}""", tool.inputSchema.toJsonString())
        assertEquals("""{"safe":true}""", tool.meta?.toJsonString())
    }

    @Test
    fun mcpResourceReadDescriptorSendsServerUriAndDecodesTextAndBlobContents() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val result = async {
            fixture.client.request(
                CodexRpc.Mcp.ReadResource,
                McpResourceReadParams(
                    server = McpServerName("github"),
                    threadId = ThreadId("thread_123"),
                    uri = McpResourceUri("github://repos/openai/codex/readme"),
                ),
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("mcpServer/resource/read", request.method)
        val params = request.params!!.jsonObject
        assertEquals("github", params["server"]?.jsonPrimitive?.contentOrNull)
        assertEquals("thread_123", params["threadId"]?.jsonPrimitive?.contentOrNull)
        assertEquals("github://repos/openai/codex/readme", params["uri"]?.jsonPrimitive?.contentOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put(
                        "contents",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("uri", "github://repos/openai/codex/readme")
                                    put("mimeType", "text/markdown")
                                    put("text", "# Codex")
                                    put(
                                        "_meta",
                                        buildJsonObject {
                                            put("etag", "abc123")
                                        },
                                    )
                                },
                            )
                            add(
                                buildJsonObject {
                                    put("uri", "github://repos/openai/codex/logo")
                                    put("mimeType", "image/png")
                                    put("blob", "iVBORw0KGgo=")
                                },
                            )
                        },
                    )
                },
            ),
        )

        val contents = result.await().contents
        val text = assertIs<McpResourceContent.Text>(contents[0])
        assertEquals(McpResourceUri("github://repos/openai/codex/readme"), text.uri)
        assertEquals("# Codex", text.text)
        assertEquals("text/markdown", text.mimeType)
        assertEquals("""{"etag":"abc123"}""", text.meta?.toJsonString())

        val blob = assertIs<McpResourceContent.Blob>(contents[1])
        assertEquals(McpResourceUri("github://repos/openai/codex/logo"), blob.uri)
        assertEquals("iVBORw0KGgo=", blob.blob)
        assertEquals("image/png", blob.mimeType)
    }

    @Test
    fun mcpOauthToolCallAndConfigReloadDescriptorsUseCurrentSchemaMethods() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val oauthResult = async {
            fixture.client.request(
                CodexRpc.Mcp.StartOauthLogin,
                McpServerOauthLoginParams(
                    name = McpServerName("github"),
                    scopes = listOf("repo", "read:user"),
                    timeoutSecs = 120,
                ),
            )
        }
        runCurrent()

        val oauth = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("mcpServer/oauth/login", oauth.method)
        val oauthParams = oauth.params!!.jsonObject
        assertEquals("github", oauthParams["name"]?.jsonPrimitive?.contentOrNull)
        assertEquals(listOf("repo", "read:user"), oauthParams["scopes"]!!.jsonArray.map { it.jsonPrimitive.contentOrNull })
        assertEquals(120, oauthParams["timeoutSecs"]?.jsonPrimitive?.intOrNull)
        fixture.transport.receive(
            JsonRpcResponse(
                oauth.id,
                result = buildJsonObject {
                    put("authorizationUrl", "https://auth.example.invalid/oauth")
                },
            ),
        )
        assertEquals("https://auth.example.invalid/oauth", oauthResult.await().authorizationUrl)

        val toolResult = async {
            fixture.client.request(
                CodexRpc.Mcp.CallTool,
                McpServerToolCallParams(
                    server = McpServerName("github"),
                    threadId = ThreadId("thread_123"),
                    tool = McpToolName("search_issues"),
                    arguments = CodexJsonPayload.parse("""{"query":"is:open"}"""),
                    meta = CodexJsonPayload.parse("""{"trace":"trace_123"}"""),
                ),
            )
        }
        runCurrent()

        val tool = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("mcpServer/tool/call", tool.method)
        val toolParams = tool.params!!.jsonObject
        assertEquals("github", toolParams["server"]?.jsonPrimitive?.contentOrNull)
        assertEquals("thread_123", toolParams["threadId"]?.jsonPrimitive?.contentOrNull)
        assertEquals("search_issues", toolParams["tool"]?.jsonPrimitive?.contentOrNull)
        assertEquals("is:open", toolParams["arguments"]!!.jsonObject["query"]?.jsonPrimitive?.contentOrNull)
        assertEquals("trace_123", toolParams["_meta"]!!.jsonObject["trace"]?.jsonPrimitive?.contentOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                tool.id,
                result = buildJsonObject {
                    put(
                        "content",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("type", "text")
                                    put("text", "No open issues")
                                },
                            )
                        },
                    )
                    put(
                        "structuredContent",
                        buildJsonObject {
                            put("total", 0)
                        },
                    )
                    put("isError", false)
                    put(
                        "_meta",
                        buildJsonObject {
                            put("durationMs", 12)
                        },
                    )
                },
            ),
        )
        val toolDecoded = toolResult.await()
        assertEquals("""[{"type":"text","text":"No open issues"}]""", toolDecoded.content.toJsonString())
        assertEquals("""{"total":0}""", toolDecoded.structuredContent?.toJsonString())
        assertEquals(false, toolDecoded.isError)
        assertEquals("""{"durationMs":12}""", toolDecoded.meta?.toJsonString())

        val reloadResult = async {
            fixture.client.request(CodexRpc.Mcp.ReloadConfig, McpConfigReloadParams)
        }
        runCurrent()

        val reload = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("config/mcpServer/reload", reload.method)
        assertNull(reload.params)
        fixture.transport.receive(JsonRpcResponse(reload.id, result = JsonObject(emptyMap())))
        assertEquals(CodexRpcUnit, reloadResult.await())
    }

    private suspend fun TestScope.connectedRpcClientFixture(
        scope: CoroutineScope,
    ): ConnectedRpcClientFixture {
        val transport = FakeJsonRpcTransport()
        val client = async {
            CodexClients.connect(
                CodexClientConnection(
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
        val client: CodexClient,
        val transport: FakeJsonRpcTransport,
    )
}
