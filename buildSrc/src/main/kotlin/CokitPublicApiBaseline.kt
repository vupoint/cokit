import java.io.File

object CokitPublicApiBaseline {
    private const val HEADER = """
# CoKit public API source baseline.
# Update with: ./gradlew updatePublicApiBaseline
"""

    fun generate(files: Iterable<File>, rootDir: File): String {
        val declarations = files
            .filter { file -> file.isFile && file.extension == "kt" }
            .sortedBy { file -> file.relativeTo(rootDir).invariantSeparatorsPath }
            .flatMap { file ->
                file.readLines()
                    .mapNotNull { line ->
                        val declaration = line.substringBefore("//").trim()
                        if (declaration.isPublicApiDeclaration()) {
                            "${file.relativeTo(rootDir).invariantSeparatorsPath}: $declaration"
                        } else {
                            null
                        }
                    }
            }

        return buildString {
            append(HEADER.trimIndent())
            appendLine()
            appendLine()
            declarations.forEach { declaration -> appendLine(declaration) }
        }
    }

    private fun String.isPublicApiDeclaration(): Boolean {
        if (isBlank() || startsWith("package ") || startsWith("import ")) return false
        if (startsWith("@") || startsWith("*")) return false
        if (contains("internal ") || contains("private ")) return false
        return classLikeDeclaration.matches(this) ||
            propertyDeclaration.containsMatchIn(this) ||
            functionDeclaration.containsMatchIn(this)
    }

    private val classLikeDeclaration = Regex(
        "^(?:data\\s+|sealed\\s+|value\\s+|enum\\s+|fun\\s+)*" +
            "(?:class|interface|object)\\b.*|" +
            "^typealias\\s+.*|" +
            "^companion\\s+object\\b.*",
    )

    private val propertyDeclaration = Regex(
        "^(?:override\\s+)?(?:val|var)\\s+[A-Za-z_][A-Za-z0-9_]*\\s*:.*",
    )

    private val functionDeclaration = Regex(
        "^(?:override\\s+)?(?:suspend\\s+)?fun\\s+[A-Za-z_][A-Za-z0-9_]*\\s*\\(.*",
    )
}
