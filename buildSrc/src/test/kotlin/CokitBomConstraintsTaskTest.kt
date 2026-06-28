import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import org.gradle.testfixtures.ProjectBuilder

class CokitBomConstraintsTaskTest {
    @Test
    fun acceptsGeneratedBomPomWithExpectedManagedDependenciesOnly() {
        val root = kotlin.io.path.createTempDirectory("cokit-bom-constraints").toFile()
        val pom = pomFile(
            root,
            """
            <project>
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>io.github.vupoint.cokit</groupId>
                    <artifactId>cokit-client</artifactId>
                    <version>0.1.0</version>
                  </dependency>
                  <dependency>
                    <groupId>io.github.vupoint.cokit</groupId>
                    <artifactId>cokit-client-jvm</artifactId>
                    <version>0.1.0</version>
                  </dependency>
                </dependencies>
              </dependencyManagement>
            </project>
            """.trimIndent(),
        )
        val task = task(root)
        task.pomFile.set(pom)
        task.expectedConstraints.set(
            listOf(
                "io.github.vupoint.cokit:cokit-client:0.1.0",
                "io.github.vupoint.cokit:cokit-client-jvm:0.1.0",
            ),
        )

        task.checkConstraints()
    }

    @Test
    fun rejectsRegularDependencies() {
        val root = kotlin.io.path.createTempDirectory("cokit-bom-constraints").toFile()
        val pom = pomFile(
            root,
            """
            <project>
              <dependencies>
                <dependency>
                  <groupId>io.github.vupoint.cokit</groupId>
                  <artifactId>cokit-client</artifactId>
                  <version>0.1.0</version>
                </dependency>
              </dependencies>
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>io.github.vupoint.cokit</groupId>
                    <artifactId>cokit-rpc</artifactId>
                    <version>0.1.0</version>
                  </dependency>
                </dependencies>
              </dependencyManagement>
            </project>
            """.trimIndent(),
        )
        val task = task(root)
        task.pomFile.set(pom)
        task.expectedConstraints.set(listOf("io.github.vupoint.cokit:cokit-client:0.1.0"))

        val failure = assertFailsWith<IllegalStateException> {
            task.checkConstraints()
        }

        assertContains(
            failure.message.orEmpty(),
            "CoKit BOM must not declare regular dependencies: [cokit-client]",
        )
    }

    @Test
    fun rejectsConstraintMismatches() {
        val root = kotlin.io.path.createTempDirectory("cokit-bom-constraints").toFile()
        val pom = pomFile(
            root,
            """
            <project>
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>io.github.vupoint.cokit</groupId>
                    <artifactId>cokit-rpc</artifactId>
                    <version>0.1.0</version>
                  </dependency>
                </dependencies>
              </dependencyManagement>
            </project>
            """.trimIndent(),
        )
        val task = task(root)
        task.pomFile.set(pom)
        task.expectedConstraints.set(listOf("io.github.vupoint.cokit:cokit-client:0.1.0"))

        val failure = assertFailsWith<IllegalStateException> {
            task.checkConstraints()
        }

        assertContains(failure.message.orEmpty(), "Unexpected CoKit BOM constraints.")
        assertContains(failure.message.orEmpty(), "io.github.vupoint.cokit:cokit-client:0.1.0")
        assertContains(failure.message.orEmpty(), "io.github.vupoint.cokit:cokit-rpc:0.1.0")
    }

    private fun task(root: File): CokitBomConstraintsTask {
        val project = ProjectBuilder.builder()
            .withProjectDir(root)
            .build()
        return project.tasks.register(
            "checkCokitBomConstraints",
            CokitBomConstraintsTask::class.java,
        ).get()
    }

    private fun pomFile(root: File, text: String): File {
        val file = root.resolve("pom-default.xml")
        file.writeText(text)
        return file
    }
}
