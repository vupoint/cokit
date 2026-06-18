import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class CokitPublicApiBaselineTest {
    @Test
    fun rendersPublicDeclarationsWithStableRelativePaths() {
        val root = kotlin.io.path.createTempDirectory("cokit-api-baseline").toFile()
        val source = root.resolve("cokit-client/src/commonMain/kotlin/io/github/cokit/client/Sample.kt")
        source.parentFile.mkdirs()
        source.writeText(
            """
            package io.github.cokit.client

            internal class InternalModel
            private class PrivateModel

            data class PublicModel(
                val name: String,
                val payload: CodexJsonPayload? = null,
            )

            fun publicFactory(name: String): PublicModel = PublicModel(name)
            """.trimIndent(),
        )

        assertEquals(
            """
            # CoKit public API source baseline.
            # Update with: ./gradlew updatePublicApiBaseline

            cokit-client/src/commonMain/kotlin/io/github/cokit/client/Sample.kt: data class PublicModel(
            cokit-client/src/commonMain/kotlin/io/github/cokit/client/Sample.kt: val name: String,
            cokit-client/src/commonMain/kotlin/io/github/cokit/client/Sample.kt: val payload: CodexJsonPayload? = null,
            cokit-client/src/commonMain/kotlin/io/github/cokit/client/Sample.kt: fun publicFactory(name: String): PublicModel = PublicModel(name)
            """.trimIndent() + "\n",
            CokitPublicApiBaseline.generate(listOf(source), root),
        )
    }
}
