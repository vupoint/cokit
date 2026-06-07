package io.github.cokit.sample.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import io.github.cokit.client.ClientInfo
import io.github.cokit.client.CodexAppServerClient
import io.github.cokit.client.CodexClientOptions
import io.github.cokit.client.CodexHostPath
import io.github.cokit.client.StartThreadRequest
import io.github.cokit.client.StartTurnRequest
import io.github.cokit.client.TurnInput
import io.github.cokit.transport.stdio.StdioCodexTransport
import kotlinx.coroutines.coroutineScope
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
        Run a minimal codex app-server thread and turn sample.

        With no arguments, the sample uses the current working directory and a
        default message. Set COKIT_CODEX_COMMAND to override the local app-server
        command. The default command is: codex app-server --stdio
    """.trimIndent()

    private val cwd by option(
        "--cwd",
        help = "App-server host path for the thread.",
    ).default(defaultCwd)

    private val message by option(
        "--message",
        help = "User message to send as turn input.",
    ).default(SampleCliDefaults.message)

    override fun run() {
        val output = runBlocking {
            runner.run(SampleOptions(cwd = cwd, message = message))
        }
        echo(output)
    }
}

interface SampleRunner {
    suspend fun run(options: SampleOptions): String
}

data class SampleOptions(
    val cwd: String,
    val message: String,
)

object SampleCliDefaults {
    const val message: String = "Say hello from the CoKit sample CLI."
}

private class CodexSampleRunner : SampleRunner {
    override suspend fun run(options: SampleOptions): String = coroutineScope {
        StdioCodexTransport(command = codexCommand()).use { transport ->
            CodexAppServerClient.connect(
                CodexClientOptions(
                    transport = transport,
                    clientInfo = ClientInfo(
                        name = "cokit_sample_cli",
                        title = "CoKit Sample CLI",
                        version = "0.1.0",
                    ),
                    scope = this,
                ),
            ).use { client ->
                val thread = client.threads.start(
                    StartThreadRequest(cwd = CodexHostPath(options.cwd)),
                )
                val turn = client.turns.start(
                    StartTurnRequest(
                        threadId = thread.id,
                        input = listOf(
                            TurnInput.Text(options.message),
                        ),
                    ),
                )

                "Started thread ${thread.id.value} and turn ${turn.id.value}"
            }
        }
    }

    private fun codexCommand(): List<String> {
        return System.getenv("COKIT_CODEX_COMMAND")
            ?.split(Regex("\\s+"))
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?: listOf("codex", "app-server", "--stdio")
    }
}
