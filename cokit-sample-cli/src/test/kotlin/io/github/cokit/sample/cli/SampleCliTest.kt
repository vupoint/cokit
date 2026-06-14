package io.github.cokit.sample.cli

import com.github.ajalt.clikt.testing.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SampleCliTest {
    @Test
    fun runsWithDefaultOptionsWhenNoArgumentsAreProvided() {
        val runner = RecordingSampleRunner()

        val result = SampleCliCommand(
            runner = runner,
            defaultCwd = "/path/to/project",
        ).test(emptyArray())

        assertEquals(0, result.statusCode)
        assertEquals("Started sample turn\n", result.stdout)
        assertEquals(
            SampleOptions(
                cwd = "/path/to/project",
                message = SampleCliDefaults.message,
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
        assertTrue(help.contains("Run a minimal codex app-server thread and turn sample."))
        assertTrue(help.contains("--cwd"))
        assertTrue(help.contains("--message"))
        assertTrue(help.contains("COKIT_CODEX_COMMAND"))
        assertTrue(!help.contains("codex app-server --stdio"))
    }

    private class RecordingSampleRunner : SampleRunner {
        var options: SampleOptions? = null

        override suspend fun run(options: SampleOptions): String {
            this.options = options
            return "Started sample turn"
        }
    }
}
