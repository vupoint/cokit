package io.github.cokit.client

import io.github.cokit.client.commands.CommandExecParams
import io.github.cokit.client.commands.CommandExecTerminalSize
import io.github.cokit.client.commands.CommandProcessId
import io.github.cokit.client.commands.CommandSandboxPolicy
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import io.github.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CommandRpcTest {
    @Test
    fun commandExecDescriptorSendsSandboxedArgvAndDecodesBufferedResult() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val result = async {
            fixture.client.request(
                CodexRpc.Command.Exec,
                CommandExecParams(
                    command = listOf("git", "status", "--short"),
                    cwd = CodexHostPath("/path/to/project"),
                    env = mapOf(
                        "CI" to "1",
                        "DEBUG" to null,
                    ),
                    outputBytesCap = 4096,
                    timeoutMs = 5_000,
                    processId = CommandProcessId("cmd_1"),
                    streamStdin = true,
                    streamStdoutStderr = true,
                    tty = true,
                    size = CommandExecTerminalSize(cols = 120, rows = 40),
                    sandboxPolicy = CommandSandboxPolicy.WorkspaceWrite(
                        writableRoots = listOf(CodexHostPath("/path/to/project")),
                        networkAccess = true,
                        excludeTmpdirEnvVar = true,
                    ),
                ),
            )
        }
        runCurrent()

        val sent = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("command/exec", sent.method)
        val params = sent.params!!.jsonObject
        assertEquals(
            listOf("git", "status", "--short"),
            params["command"]!!.jsonArray.map { it.jsonPrimitive.contentOrNull },
        )
        assertEquals("/path/to/project", params["cwd"]?.jsonPrimitive?.contentOrNull)
        assertEquals("4096", params["outputBytesCap"]?.jsonPrimitive?.contentOrNull)
        assertEquals("5000", params["timeoutMs"]?.jsonPrimitive?.contentOrNull)
        assertEquals("cmd_1", params["processId"]?.jsonPrimitive?.contentOrNull)
        assertEquals(true, params["streamStdin"]?.jsonPrimitive?.booleanOrNull)
        assertEquals(true, params["streamStdoutStderr"]?.jsonPrimitive?.booleanOrNull)
        assertEquals(true, params["tty"]?.jsonPrimitive?.booleanOrNull)

        val env = params["env"]!!.jsonObject
        assertEquals("1", env["CI"]?.jsonPrimitive?.contentOrNull)
        assertEquals(JsonNull, env["DEBUG"])

        val size = params["size"]!!.jsonObject
        assertEquals("120", size["cols"]?.jsonPrimitive?.contentOrNull)
        assertEquals("40", size["rows"]?.jsonPrimitive?.contentOrNull)

        val sandbox = params["sandboxPolicy"]!!.jsonObject
        assertEquals("workspaceWrite", sandbox["type"]?.jsonPrimitive?.contentOrNull)
        assertEquals(true, sandbox["networkAccess"]?.jsonPrimitive?.booleanOrNull)
        assertEquals(true, sandbox["excludeTmpdirEnvVar"]?.jsonPrimitive?.booleanOrNull)
        assertTrue("excludeSlashTmp" !in sandbox)
        assertEquals(
            "/path/to/project",
            sandbox["writableRoots"]!!.jsonArray.single().jsonPrimitive.contentOrNull,
        )

        fixture.transport.receive(
            JsonRpcResponse(
                sent.id,
                result = buildJsonObject {
                    put("exitCode", 0)
                    put("stdout", "clean\n")
                    put("stderr", "")
                },
            ),
        )

        assertEquals(0, result.await().exitCode)
        assertEquals("clean\n", result.await().stdout)
        assertEquals("", result.await().stderr)
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
