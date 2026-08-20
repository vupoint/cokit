import java.io.File
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import org.gradle.testfixtures.ProjectBuilder

class CodexSchemaGenerateTaskTest {
    @Test
    fun provenanceValidationRejectsMissingSchemaDigest() {
        val root = kotlin.io.path.createTempDirectory("cokit-schema-provenance").toFile()
        val provenance = root.resolve("codex-schema-provenance.properties").apply {
            writeText(
                """
                codexVersion=codex-cli 0.146.0
                upstreamCommit=e363b08c9175ac1cbe5893615dd2cb9ddf95043b
                stableCommand=codex app-server generate-json-schema --out build/generated/codex-schema/stable
                experimentalCommand=codex app-server generate-json-schema --out build/generated/codex-schema/experimental --experimental
                generatedAt=2026-08-20T12:00:00+09:00
                """.trimIndent(),
            )
        }
        val task = task(root, provenance)

        val failure = assertFailsWith<InvocationTargetException> {
            task.invokeProvenanceValidation()
        }

        assertContains(
            failure.cause?.message.orEmpty(),
            "missing required key: stableSchemaSha256",
        )
    }

    @Test
    fun installedCodexVersionMustMatchRecordedVersion() {
        val validationMethod = runCatching {
            Class.forName("CodexSchemaGenerateTaskKt").getDeclaredMethod(
                "requireMatchingCodexVersion",
                String::class.java,
                String::class.java,
            )
        }.getOrNull()

        assertNotNull(validationMethod, "Codex schema generation must validate the installed Codex version.")
        val failure = assertFailsWith<InvocationTargetException> {
            validationMethod.invoke(null, "codex-cli 0.146.0", "codex-cli 0.145.0")
        }
        assertContains(
            failure.cause?.message.orEmpty(),
            "Recorded Codex version 'codex-cli 0.146.0' does not match installed version 'codex-cli 0.145.0'",
        )
    }

    private fun task(root: File, provenance: File): CodexSchemaGenerateTask {
        val project = ProjectBuilder.builder()
            .withProjectDir(root)
            .build()
        return project.tasks.register(
            "generateStableCodexSchema",
            CodexSchemaGenerateTask::class.java,
        ).get().apply {
            outputDirectory.set(project.layout.buildDirectory.dir("generated/codex-schema/stable"))
            experimental.set(false)
            provenanceFile.set(provenance)
            provenanceCommandKey.set("stableCommand")
            recordedCommand.set("codex app-server generate-json-schema --out build/generated/codex-schema/stable")
        }
    }

    private fun CodexSchemaGenerateTask.invokeProvenanceValidation() {
        val method = CodexSchemaGenerateTask::class.java.getDeclaredMethod("validateProvenance")
        method.isAccessible = true
        method.invoke(this)
    }
}
