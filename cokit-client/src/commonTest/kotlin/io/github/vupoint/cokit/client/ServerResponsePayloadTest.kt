@file:OptIn(ExperimentalCodexApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.mcp.McpElicitationResponse
import io.github.vupoint.cokit.client.tools.DynamicToolCallOutputContent
import io.github.vupoint.cokit.client.tools.DynamicToolCallResponse
import io.github.vupoint.cokit.protocol.CodexProtocolJson
import io.github.vupoint.cokit.protocol.JsonRpcId
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

class ServerResponsePayloadTest {
    @Test
    fun dynamicToolResponseDetachesUnknownContentFromHandlerOwnedJson() = runTest {
        val (client, transport) = connect()
        val values = mutableListOf<JsonElement>(JsonPrimitive(1))
        val fields = mutableMapOf<String, JsonElement>(
            "type" to JsonPrimitive("futureContent"),
            "values" to JsonArray(values),
        )
        val payload = CodexProtocolJson.decodeFromJsonElement(CodexJsonPayloadSerializer, JsonObject(fields))
        client.registerDynamicToolCallHandler {
            DynamicToolCallResponse(listOf(DynamicToolCallOutputContent.Unknown(payload)), success = true)
        }

        transport.receive(JsonRpcRequest(
            JsonRpcId.StringId("rpc_tool"),
            "item/tool/call",
            json("""{"threadId":"thr_test","turnId":"turn_test","callId":"call_test","tool":"lookup","arguments":null}"""),
        ))
        runCurrent()
        val response = transport.sent.last() as JsonRpcResponse
        val expected = json("""{"contentItems":[{"type":"futureContent","values":[1]}],"success":true}""")
        assertEquals(JsonRpcId.StringId("rpc_tool"), response.id)
        assertEquals(expected, response.result)

        values += JsonPrimitive(2)
        fields["type"] = JsonPrimitive("changedContent")
        assertEquals(json("""{"type":"changedContent","values":[1,2]}"""), json(payload.toJsonString()))
        assertEquals(expected, response.result)
        client.close()
    }

    @Test
    fun mcpAcceptPreservesJsonKindsAndAbsentVersusNullMetadata() = runTest {
        val (client, transport) = connect()
        val contents = listOf("null", "{}", "[]", "\"answer\"", "true", "1.25", """{"future":[1,null]}""")
        for ((index, content) in contents.withIndex()) {
            for (includeMetadata in listOf(false, true)) {
                client.registerMcpElicitationHandler {
                    McpElicitationResponse.Accept(
                        content = CodexJsonPayload.parse(content),
                        meta = if (includeMetadata) CodexJsonPayload.parse("null") else null,
                    )
                }
                val id = JsonRpcId.Number(index.toLong() * 2 + if (includeMetadata) 1 else 0)
                transport.receive(JsonRpcRequest(
                    id,
                    "mcpServer/elicitation/request",
                    json("""{"serverName":"test","threadId":"thr_test","mode":"form","message":"Provide a value.","requestedSchema":{"type":"object","properties":{}}}"""),
                ))
                runCurrent()
                val response = transport.sent.last() as JsonRpcResponse
                val metadata = if (includeMetadata) ",\"_meta\":null" else ""
                assertEquals(id, response.id)
                assertEquals(json("""{"action":"accept","content":$content$metadata}"""), response.result)
            }
        }
        client.close()
    }

    private suspend fun TestScope.connect(): Pair<CodexClient, FakeJsonRpcTransport> {
        val transport = FakeJsonRpcTransport()
        val pending = async {
            CodexClients.connect(CodexClientConnection(
                transport,
                ClientInfo("cokit_test", "CoKit Test", "0.1.0"),
                backgroundScope,
                InitializeCapabilities(experimentalApi = true),
            ))
        }
        runCurrent()
        val initialize = transport.sent.single() as JsonRpcRequest
        transport.receive(JsonRpcResponse(initialize.id, json("{}")))
        return pending.await() to transport
    }

    private fun json(value: String): JsonElement = CodexProtocolJson.parseToJsonElement(value)
}
