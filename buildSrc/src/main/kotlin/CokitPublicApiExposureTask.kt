import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class CokitPublicApiExposureTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @TaskAction
    fun checkExposure() {
        val violations = CokitPublicApiChecks.findViolations(
            sourceFiles.files,
            rootDirectory.get().asFile,
        )
        check(violations.isEmpty()) {
            buildString {
                appendLine("Primary client APIs must not expose raw JSON or JSON-RPC envelope types.")
                appendLine("Use typed models or CodexJsonPayload for documented compatibility fields.")
                violations.forEach { violation ->
                    appendLine(
                        "${violation.relativePath}:${violation.lineNumber}: ${violation.typeName}: ${violation.line}",
                    )
                }
            }
        }
    }
}
