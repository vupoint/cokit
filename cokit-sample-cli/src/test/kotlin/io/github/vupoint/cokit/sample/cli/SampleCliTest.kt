package io.github.vupoint.cokit.sample.cli

import com.github.ajalt.clikt.testing.test
import io.github.vupoint.cokit.protocol.JsonRpcNotification
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.testing.FakeJsonRpcTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SampleCliTest {
    @Test
    fun runsWithDefaultOptionsWhenNoArgumentsAreProvided() {
        val runner = RecordingSampleRunner()

        val result = SampleCliCommand(
            runner = runner,
            defaultCwd = "/path/to/project",
        ).test(emptyArray())

        assertEquals(0, result.statusCode)
        assertEquals("Started sample turn\nAssistant: Hello from Codex\n", result.stdout)
        assertEquals(
            SampleOptions(
                cwd = "/path/to/project",
                message = SampleCliDefaults.MESSAGE,
            ),
            runner.options,
        )
    }

    @Test
    fun usesOptionalOverridesWhenProvided() {
        val runner = RecordingSampleRunner()

        val result = SampleCliCommand(
            runner = runner,
            defaultCwd = "/path/to/project",
        ).test(
            arrayOf(
                "--cwd",
                "/another/project",
                "--message",
                "Summarize this repository",
            ),
        )

        assertEquals(0, result.statusCode)
        assertEquals(
            SampleOptions(
                cwd = "/another/project",
                message = "Summarize this repository",
            ),
            runner.options,
        )
    }

    @Test
    fun generatedHelpDescribesNoArgumentUsage() {
        val help = SampleCliCommand(
            runner = RecordingSampleRunner(),
            defaultCwd = "/path/to/project",
        ).getFormattedHelp().orEmpty()

        assertTrue(help.contains("Usage:"))
        assertTrue(help.contains("Run a minimal codex app-server thread and stream the assistant response."))
        assertTrue(help.contains("--cwd"))
        assertTrue(help.contains("--message"))
    }

    @Test
    fun codexRunnerUsesTypedRequestsAndStreamsAssistantDeltasUntilTurnCompletes() = runTest {
        val transport = FakeJsonRpcTransport()
        val output = RecordingSampleOutput()

        val run = async {
            CodexSampleRunner(
                transportFactory = { transport },
            ).run(
                SampleOptions(
                    cwd = "/path/to/project",
                    message = "Say hello",
                ),
                output,
            )
        }

        runCurrent()
        val initialize = transport.sent.single() as JsonRpcRequest
        assertEquals("initialize", initialize.method)
        transport.receive(JsonRpcResponse(initialize.id, result = JsonObject(emptyMap())))

        runCurrent()
        val threadStart = transport.sent.requests().last()
        assertEquals("thread/start", threadStart.method)
        assertEquals(
            "/path/to/project",
            threadStart.params!!.jsonObject["cwd"]!!.jsonPrimitive.content,
        )
        transport.receive(
            JsonRpcResponse(
                threadStart.id,
                result = buildJsonObject {
                    put("thread", buildJsonObject { put("id", "thr_123") })
                },
            ),
        )

        runCurrent()
        val turnStart = transport.sent.requests().last()
        assertEquals("turn/start", turnStart.method)
        val turnParams = turnStart.params!!.jsonObject
        assertEquals("thr_123", turnParams["threadId"]!!.jsonPrimitive.content)
        assertEquals(
            "Say hello",
            turnParams["input"]!!.jsonArray.single().jsonObject["text"]!!.jsonPrimitive.content,
        )

        transport.receive(agentMessageDelta("Hello"))
        transport.receive(agentMessageDelta(" from Codex"))
        transport.receive(turnCompleted())
        transport.receive(
            JsonRpcResponse(
                turnStart.id,
                result = buildJsonObject {
                    put("turn", turnPayload(id = "turn_123", status = "inProgress"))
                },
            ),
        )

        run.await()
        assertEquals(
            "Started thread thr_123 and turn turn_123\n\nAssistant:\nHello from Codex\n",
            output.content,
        )
    }

    @Test
    fun codexRunnerReportsTurnFailure() = runTest {
        val transport = FakeJsonRpcTransport()
        val output = RecordingSampleOutput()

        val result = supervisorScope {
            val run = async {
                CodexSampleRunner(
                    transportFactory = { transport },
                ).run(
                    SampleOptions(
                        cwd = "/path/to/project",
                        message = "Say hello",
                    ),
                    output,
                )
            }

            runCurrent()
            val initialize = transport.sent.single() as JsonRpcRequest
            transport.receive(JsonRpcResponse(initialize.id, result = JsonObject(emptyMap())))

            runCurrent()
            val threadStart = transport.sent.requests().last()
            transport.receive(
                JsonRpcResponse(
                    threadStart.id,
                    result = buildJsonObject {
                        put("thread", buildJsonObject { put("id", "thr_123") })
                    },
                ),
            )

            runCurrent()
            val turnStart = transport.sent.requests().last()
            transport.receive(turnFailed())
            transport.receive(
                JsonRpcResponse(
                    turnStart.id,
                    result = buildJsonObject {
                        put("turn", turnPayload(id = "turn_123", status = "inProgress"))
                    },
                ),
            )

            runCatching { run.await() }
        }

        assertTrue(result.isFailure)
        assertEquals(
            "Started thread thr_123 and turn turn_123\n\nAssistant:\n",
            output.content,
        )
        assertEquals("Turn turn_123 failed: model request failed", result.exceptionOrNull()?.message)
    }

    private class RecordingSampleRunner : SampleRunner {
        var options: SampleOptions? = null

        override suspend fun run(options: SampleOptions, output: SampleOutput) {
            this.options = options
            output.line("Started sample turn")
            output.text("Assistant: ")
            output.text("Hello from Codex")
            output.line()
        }
    }

    private class RecordingSampleOutput : SampleOutput {
        private val builder = StringBuilder()

        val content: String
            get() = builder.toString()

        override fun text(value: String) {
            builder.append(value)
        }

        override fun line(value: String) {
            builder.append(value)
            builder.append('\n')
        }
    }

    private fun List<Any>.requests(): List<JsonRpcRequest> = filterIsInstance<JsonRpcRequest>()

    private fun agentMessageDelta(delta: String) = JsonRpcNotification(
        method = "item/agentMessage/delta",
        params = buildJsonObject {
            put("threadId", "thr_123")
            put("turnId", "turn_123")
            put("itemId", "item_msg")
            put("delta", delta)
        },
    )

    private fun turnCompleted() = JsonRpcNotification(
        method = "turn/completed",
        params = buildJsonObject {
            put("turn", turnPayload(id = "turn_123", status = "completed"))
        },
    )

    private fun turnFailed() = JsonRpcNotification(
        method = "turn/completed",
        params = buildJsonObject {
            put(
                "turn",
                buildJsonObject {
                    put("id", "turn_123")
                    put("status", "failed")
                    put("items", buildJsonArray {})
                    put(
                        "error",
                        buildJsonObject { put("message", "model request failed") },
                    )
                },
            )
        },
    )

    private fun turnPayload(id: String, status: String) = buildJsonObject {
        put("id", id)
        put("status", status)
        put("items", buildJsonArray {})
    }
}
