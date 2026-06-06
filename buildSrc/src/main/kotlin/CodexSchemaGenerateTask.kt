import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

abstract class CodexSchemaGenerateTask @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val experimental: Property<Boolean>

    @TaskAction
    fun generate() {
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
}
