import java.io.File

object CokitPrimaryDocsAlignment {
    data class Violation(
        val relativePath: String,
        val lineNumber: Int,
        val reason: String,
        val match: String,
    )

    private val rawMethodPattern = Regex(
        "\\b(?:account|app|apps|attestation|command|config|conversation|filesystem|hooks|item|" +
            "marketplace|mcp|mcpServer|model|plugin|review|remote|serverRequest|thread|turn|userInput)" +
            "/[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*\\b",
    )
    private val directStdioCommandPatterns = listOf(
        Regex("listOf\\(\\s*\"codex\"\\s*,\\s*\"app-server\"\\s*,\\s*\"--stdio\"\\s*\\)"),
        Regex("codex\\s+app-server\\s+--stdio"),
        Regex("\"codex\"\\s*,\\s*\"app-server\"\\s*,\\s*\"--stdio\""),
    )

    fun findViolations(files: Iterable<File>, rootDir: File? = null): List<Violation> {
        return files
            .filter { file -> file.isFile && file.extension in setOf("kt", "md") }
            .flatMap { file ->
                val relativePath = file.relativeDisplayPath(rootDir)
                if (relativePath.allowsRawProtocolReferences()) {
                    emptyList()
                } else {
                    file.readLines().mapIndexedNotNull { index, line ->
                        violationForLine(relativePath, index + 1, line)
                    }
                }
            }
    }

    private fun violationForLine(relativePath: String, lineNumber: Int, line: String): Violation? {
        if (line.isBlank()) return null
        directStdioCommandPatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(line)?.value
        }?.let { match ->
            return Violation(
                relativePath = relativePath,
                lineNumber = lineNumber,
                reason = "direct stdio command list",
                match = match,
            )
        }
        val rawMethod = rawMethodPattern.find(line)?.value ?: return null
        return Violation(
            relativePath = relativePath,
            lineNumber = lineNumber,
            reason = "raw app-server method string",
            match = rawMethod,
        )
    }

    private fun String.allowsRawProtocolReferences(): Boolean {
        return startsWith("docs/protocol-") ||
            startsWith("cokit-protocol/src/commonTest/resources/fixtures/")
    }

    private fun File.relativeDisplayPath(rootDir: File?): String {
        return if (rootDir == null) {
            name
        } else {
            relativeTo(rootDir).invariantSeparatorsPath
        }
    }
}
