import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import org.gradle.testfixtures.ProjectBuilder

class CokitPublicApiExposureTaskTest {
    @Test
    fun reportsForbiddenPrimaryApiTypesWithRelativePaths() {
        val root = kotlin.io.path.createTempDirectory("cokit-public-api-exposure").toFile()
        val source = sourceFile(
            root,
            "cokit-client/src/commonMain/kotlin/io/github/vupoint/cokit/client/BadModels.kt",
            """
            package io.github.vupoint.cokit.client

            import kotlinx.serialization.json.JsonElement

            data class BadModel(
                val payload: JsonElement,
            )
            """.trimIndent(),
        )
        val task = task(root)
        task.sourceFiles.from(source)

        val failure = assertFailsWith<IllegalStateException> {
            task.checkExposure()
        }

        assertContains(
            failure.message.orEmpty(),
            "Primary client APIs must not expose raw JSON or JSON-RPC envelope types.",
        )
        assertContains(
            failure.message.orEmpty(),
            "Use typed models or CodexJsonPayload for documented compatibility fields.",
        )
        assertContains(
            failure.message.orEmpty(),
            "cokit-client/src/commonMain/kotlin/io/github/vupoint/cokit/client/BadModels.kt:6: " +
                "JsonElement: val payload: JsonElement,",
        )
    }

    @Test
    fun allowsCompatibilityPayloads() {
        val root = kotlin.io.path.createTempDirectory("cokit-public-api-exposure").toFile()
        val source = sourceFile(
            root,
            "cokit-client/src/commonMain/kotlin/io/github/vupoint/cokit/client/GoodModels.kt",
            """
            package io.github.vupoint.cokit.client

            data class GoodModel(
                val payload: CodexJsonPayload,
            )
            """.trimIndent(),
        )
        val task = task(root)
        task.sourceFiles.from(source)

        task.checkExposure()
    }

    private fun task(root: File): CokitPublicApiExposureTask {
        val project = ProjectBuilder.builder()
            .withProjectDir(root)
            .build()
        return project.tasks.register(
            "checkPublicApiExposure",
            CokitPublicApiExposureTask::class.java,
        ).get().apply {
            rootDirectory.set(project.layout.projectDirectory)
        }
    }

    private fun sourceFile(root: File, path: String, text: String): File {
        val file = root.resolve(path)
        file.parentFile.mkdirs()
        file.writeText(text)
        return file
    }
}
