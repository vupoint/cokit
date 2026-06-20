import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CokitPrimaryDocsAlignmentTest {
    @Test
    fun reportsRawMethodStringsAndDirectStdioCommandListsInPrimaryExamples() {
        val root = kotlin.io.path.createTempDirectory("cokit-docs-alignment").toFile()
        val readme = sourceFile(
            root,
            "README.md",
            """
            # CoKit

            Avoid sending raw `thread/start` method strings from examples.
            """.trimIndent(),
        )
        val sample = sourceFile(
            root,
            "cokit-sample-cli/src/main/kotlin/io/github/vupoint/cokit/sample/cli/Main.kt",
            """
            package io.github.vupoint.cokit.sample.cli

            val command = listOf("codex", "app-server", "--stdio")
            """.trimIndent(),
        )

        val violations = CokitPrimaryDocsAlignment.findViolations(listOf(readme, sample), root)

        assertEquals(
            listOf(
                "README.md:3 raw app-server method string: thread/start",
                "cokit-sample-cli/src/main/kotlin/io/github/vupoint/cokit/sample/cli/Main.kt:3 direct stdio command list: listOf(\"codex\", \"app-server\", \"--stdio\")",
            ),
            violations.map { violation ->
                "${violation.relativePath}:${violation.lineNumber} ${violation.reason}: ${violation.match}"
            },
        )
    }

    @Test
    fun allowsTypedDescriptorsAndProtocolCompatibilityDocs() {
        val root = kotlin.io.path.createTempDirectory("cokit-docs-alignment").toFile()
        val gettingStarted = sourceFile(
            root,
            "docs/getting-started.md",
            """
            val turn = client.request(
                CodexRpc.Turn.Start,
                TurnStartParams(threadId = thread.id, input = listOf(TurnInput.Text("Hello"))),
            )
            """.trimIndent(),
        )
        val compatibility = sourceFile(
            root,
            "docs/protocol-compatibility.md",
            "Compatibility docs may mention `thread/start` directly.",
        )

        assertTrue(
            CokitPrimaryDocsAlignment.findViolations(listOf(gettingStarted, compatibility), root).isEmpty(),
        )
    }

    private fun sourceFile(root: File, path: String, text: String): File {
        val file = root.resolve(path)
        file.parentFile.mkdirs()
        file.writeText(text)
        return file
    }
}
