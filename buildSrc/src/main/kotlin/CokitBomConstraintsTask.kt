import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Element

abstract class CokitBomConstraintsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val pomFile: RegularFileProperty

    @get:Input
    abstract val expectedConstraints: ListProperty<String>

    @TaskAction
    fun checkConstraints() {
        val expected = expectedConstraints.get()
            .map { constraint -> CokitBomConstraint.parse(constraint) }
            .toSet()
        val actual = parseCokitBomConstraints(pomFile.get().asFile)

        check(actual == expected) {
            "Unexpected CoKit BOM constraints. " +
                "expected=${expected.sortedBy { it.artifactId }} " +
                "actual=${actual.sortedBy { it.artifactId }}"
        }
    }

    private fun parseCokitBomConstraints(pomFile: File): Set<CokitBomConstraint> {
        check(pomFile.isFile) {
            "Generated BOM POM is missing: ${pomFile.relativeTo(project.rootDir)}"
        }

        val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            isExpandEntityReferences = false
        }
        val document = documentBuilderFactory.newDocumentBuilder().parse(pomFile)
        val projectElement = document.documentElement
        val regularDependencies = projectElement
            .directChildElements("dependencies")
            .flatMap { dependencies -> dependencies.directChildElements("dependency") }
        check(regularDependencies.isEmpty()) {
            "CoKit BOM must not declare regular dependencies: ${regularDependencies.map { it.childText("artifactId") }}"
        }

        val managedDependencies = projectElement
            .singleDirectChild("dependencyManagement")
            .singleDirectChild("dependencies")
            .directChildElements("dependency")

        return managedDependencies
            .map { dependency ->
                CokitBomConstraint(
                    groupId = dependency.childText("groupId"),
                    artifactId = dependency.childText("artifactId"),
                    version = dependency.childText("version"),
                )
            }
            .toSet()
    }

    private data class CokitBomConstraint(
        val groupId: String,
        val artifactId: String,
        val version: String,
    ) {
        override fun toString(): String = "$groupId:$artifactId:$version"

        companion object {
            fun parse(value: String): CokitBomConstraint {
                val parts = value.split(":")
                check(parts.size == 3) {
                    "Expected BOM constraint in groupId:artifactId:version format, got: $value"
                }
                return CokitBomConstraint(
                    groupId = parts[0],
                    artifactId = parts[1],
                    version = parts[2],
                )
            }
        }
    }

    private fun Element.childText(tagName: String): String {
        val nodes = getElementsByTagName(tagName)
        check(nodes.length > 0) {
            "Missing <$tagName> in BOM dependency node."
        }
        return nodes.item(0).textContent.trim()
    }

    private fun Element.directChildElements(tagName: String): List<Element> =
        (0 until childNodes.length)
            .mapNotNull { index -> childNodes.item(index) as? Element }
            .filter { child -> child.tagName == tagName }

    private fun Element.singleDirectChild(tagName: String): Element {
        val children = directChildElements(tagName)
        check(children.size == 1) {
            "Expected exactly one direct <$tagName> child in <${this.tagName}>, found ${children.size}."
        }
        return children.single()
    }
}
