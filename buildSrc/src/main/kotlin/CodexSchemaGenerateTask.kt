import java.util.Properties
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

abstract class CodexSchemaGenerateTask @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val experimental: Property<Boolean>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val provenanceFile: RegularFileProperty

    @get:Input
    abstract val provenanceCommandKey: Property<String>

    @get:Input
    abstract val recordedCommand: Property<String>

    @TaskAction
    fun generate() {
        validateProvenance()

        val outputDir = outputDirectory.get().asFile
        outputDir.mkdirs()

        val args = mutableListOf(
            "app-server",
            "generate-json-schema",
            "--out",
            outputDir.absolutePath,
        )
        if (experimental.get()) {
            args += "--experimental"
        }

        execOperations.exec {
            executable = "codex"
            args(args)
        }
    }

    private fun validateProvenance() {
        val file = provenanceFile.get().asFile
        check(file.isFile) {
            "Schema provenance file is missing: ${file.path}"
        }

        val properties = Properties()
        file.inputStream().use { stream -> properties.load(stream) }

        requiredProvenanceKeys.forEach { key ->
            check(!properties.getProperty(key).isNullOrBlank()) {
                "Schema provenance file ${file.path} is missing required key: $key"
            }
        }

        val commandKey = provenanceCommandKey.get()
        val actualCommand = properties.getProperty(commandKey)
        check(actualCommand == recordedCommand.get()) {
            "Schema provenance $commandKey is stale. Expected '${recordedCommand.get()}' but found '$actualCommand'."
        }
    }

    private companion object {
        val requiredProvenanceKeys = setOf(
            "codexVersion",
            "upstreamCommit",
            "stableCommand",
            "experimentalCommand",
            "generatedAt",
        )
    }
}
