package io.github.vupoint.cokit.sample.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import io.github.vupoint.cokit.client.ClientInfo
import io.github.vupoint.cokit.client.CodexHostPath
import io.github.vupoint.cokit.client.CodexNotification
import io.github.vupoint.cokit.client.CodexRpc
import io.github.vupoint.cokit.client.CodexClients
import io.github.vupoint.cokit.client.CodexClientConnection
import io.github.vupoint.cokit.client.ThreadStartParams
import io.github.vupoint.cokit.client.TurnInput
import io.github.vupoint.cokit.client.TurnStartParams
import io.github.vupoint.cokit.rpc.JsonRpcTransport
import io.github.vupoint.cokit.transport.stdio.StdioCodexTransport
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    SampleCliCommand().main(args)
}

class SampleCliCommand(
    private val runner: SampleRunner = CodexSampleRunner(),
    defaultCwd: String = System.getProperty("user.dir"),
) : CliktCommand(
    name = "cokit-sample-cli",
) {
    override fun help(context: Context): String = """
        Run a minimal codex app-server thread and stream the assistant response.

        With no arguments, the sample uses the current working directory and a
        default message.
    """.trimIndent()

    private val cwd by option(
        "--cwd",
        help = "App-server host path for the thread.",
    ).default(defaultCwd)

    private val message by option(
        "--message",
        help = "User message to send as turn input.",
    ).default(SampleCliDefaults.MESSAGE)

    override fun run() {
        try {
            runBlocking {
                runner.run(
                    SampleOptions(cwd = cwd, message = message),
                    object : SampleOutput {
                        override fun text(value: String) {
                            echo(value, trailingNewline = false)
                        }

                        override fun line(value: String) {
                            echo(value)
                        }
                    },
                )
            }
        } catch (error: SampleRunException) {
            throw CliktError(error.message ?: "Sample run failed.")
        }
    }
}

interface SampleRunner {
    suspend fun run(options: SampleOptions, output: SampleOutput)
}

interface SampleOutput {
    fun text(value: String)

    fun line(value: String = "")
}

data class SampleOptions(
    val cwd: String,
    val message: String,
)

object SampleCliDefaults {
    const val MESSAGE: String = "Say hello from the CoKit sample CLI."
}

internal class CodexSampleRunner(
    private val transportFactory: () -> JsonRpcTransport = { StdioCodexTransport() },
) : SampleRunner {
    override suspend fun run(options: SampleOptions, output: SampleOutput): Unit = coroutineScope {
        CodexClients.connect(
            CodexClientConnection(
                transport = transportFactory(),
                clientInfo = ClientInfo(
                    name = "cokit_sample_cli",
                    title = "CoKit Sample CLI",
                    version = "0.1.0",
                ),
                scope = this,
            ),
        ).use { client ->
            val events = Channel<CodexNotification>(Channel.UNLIMITED)
            val eventCollector = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    client.notifications.collect { event ->
                        events.send(event)
                    }
                } finally {
                    events.close()
                }
            }

            try {
                val thread = client.request(
                    CodexRpc.Thread.Start,
                    ThreadStartParams(cwd = CodexHostPath(options.cwd)),
                ).thread
                val turn = client.request(
                    CodexRpc.Turn.Start,
                    TurnStartParams(
                        threadId = thread.id,
                        input = listOf(TurnInput.Text(options.message)),
                    ),
                ).turn

                output.line("Started thread ${thread.id.value} and turn ${turn.id.value}")
                output.line()
                output.line("Assistant:")

                streamAssistantResponse(
                    turnId = turn.id,
                    events = events,
                    output = output,
                )
            } finally {
                eventCollector.cancelAndJoin()
            }
        }
    }
}
