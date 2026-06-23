import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class CokitBomPublicationTest {
    private val expectedPublishedLibraries = listOf(
        "cokit-protocol",
        "cokit-rpc",
        "cokit-client",
        "cokit-transport-stdio",
        "cokit-transport-websocket",
        "cokit-testing",
    )

    @Test
    fun declaresBomPlatformForPublishedLibrariesOnly() {
        val root = repositoryRoot()
        val settings = root.resolve("settings.gradle.kts").readText()
        val rootBuild = root.resolve("build.gradle.kts").readText()
        val bomBuild = root.resolve("cokit-bom/build.gradle.kts")

        assertTrue(settings.contains("\":cokit-bom\""), "settings.gradle.kts must include :cokit-bom")
        assertTrue(bomBuild.isFile, "cokit-bom/build.gradle.kts must exist")

        val bomBuildText = bomBuild.readText()
        assertTrue(bomBuildText.contains("java-platform"), "cokit-bom must apply the java-platform plugin")
        assertTrue(
            bomBuildText.contains("JavaPlatform()"),
            "cokit-bom must configure Vanniktech JavaPlatform publishing",
        )
        expectedPublishedLibraries.forEach { artifact ->
            assertTrue(
                bomBuildText.contains("api(project(\":$artifact\"))"),
                "cokit-bom must constrain :$artifact",
            )
        }
        expectedPublishedLibraries.forEach { artifact ->
            assertTrue(
                bomBuildText.contains("api(\"\${project.group}:$artifact-jvm:\${project.version}\")"),
                "cokit-bom must constrain the published JVM artifact $artifact-jvm",
            )
        }
        assertTrue(
            !bomBuildText.contains("cokit-sample-cli"),
            "cokit-bom must not constrain the unpublished sample CLI",
        )
        assertTrue(
            rootBuild.contains(":cokit-bom"),
            "root publishing checks must know about the BOM project",
        )
    }

    private fun repositoryRoot(): File {
        return generateSequence(File(".").canonicalFile) { file -> file.parentFile }
            .first { file ->
                file.resolve("settings.gradle.kts").isFile &&
                    file.resolve("buildSrc/build.gradle.kts").isFile
            }
    }
}
