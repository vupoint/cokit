package io.github.vupoint.cokit.client

import java.io.File
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodexRpcCoverageTest {
    @Test
    fun compatibilityNotesReportCurrentInventoryCoverage() {
        val inventory = loadDocument("docs/protocol-inventory.md")
        val compatibility = loadDocument("docs/protocol-compatibility.md")
        val descriptorCount = codexRpcDescriptorMethods().size
        val inventoryDescriptorCount = inventoryDescriptorMethods(inventory).size

        assertEquals(
            descriptorCount,
            inventoryDescriptorCount,
            "Current modeled request descriptor rows must match public CodexRpc descriptors.",
        )

        val expectedBlock = CoverageSummary(
            requestGroups = inventoryStatusCounts(inventory, "## Request Groups"),
            notificationGroups = inventoryStatusCounts(inventory, "## Notification Groups"),
            serverRequestGroups = inventoryStatusCounts(inventory, "## Server-Request Groups"),
            modeledRequestDescriptors = descriptorCount,
        ).toCompatibilityBlock()

        assertEquals(
            expectedBlock,
            compatibilityCoverageBlock(compatibility),
            "docs/protocol-compatibility.md must include the generated coverage summary.",
        )
    }

    private fun codexRpcDescriptorMethods(): Set<String> {
        val methods = CodexRpc::class.java.declaredClasses
            .flatMap { holder -> holder.codexRpcMethodFields() }
            .map { descriptor -> descriptor.method }
            .toSortedSet()

        assertTrue(methods.isNotEmpty(), "CodexRpc should expose at least one request descriptor.")
        return methods
    }

    private fun Class<*>.codexRpcMethodFields(): List<CodexRpcMethod<*, *>> {
        val instance = declaredFields
            .firstOrNull { field -> field.name == "INSTANCE" }
            ?.also { field -> field.isAccessible = true }
            ?.get(null)

        return declaredFields
            .filter { field -> field.type == CodexRpcMethod::class.java }
            .map { field ->
                field.isAccessible = true
                val receiver = if (Modifier.isStatic(field.modifiers)) null else instance
                field.get(receiver) as CodexRpcMethod<*, *>
            }
    }

    private fun inventoryDescriptorMethods(inventory: String): Set<String> {
        val section = inventorySection(inventory, "## Current Modeled Request Descriptors")
        return Regex("""^\|\s*`CodexRpc\.[^`]+`\s*\|\s*`([^`]+)`\s*\|""", RegexOption.MULTILINE)
            .findAll(section)
            .map { match -> match.groupValues[1] }
            .toSortedSet()
    }

    private fun inventoryStatusCounts(inventory: String, sectionTitle: String): Map<String, Int> {
        val counts = coverageStatuses.associateWith { 0 }.toMutableMap()
        val section = inventorySection(inventory, sectionTitle)
        val rows = Regex(
            """^\|\s*[^|]+\|\s*`?(modeled|partial|deferred|experimental)`?\s*\|""",
            RegexOption.MULTILINE,
        ).findAll(section).toList()

        assertTrue(rows.isNotEmpty(), "$sectionTitle must include at least one inventory row.")

        rows.forEach { row ->
            val status = row.groupValues[1]
            counts[status] = counts.getValue(status) + 1
        }
        return counts
    }

    private fun inventorySection(document: String, sectionTitle: String): String {
        val section = document.substringAfter(sectionTitle, missingDelimiterValue = "")
        assertTrue(section.isNotBlank(), "$sectionTitle section is required.")
        return section.substringBefore("\n## ")
    }

    private fun compatibilityCoverageBlock(compatibility: String): String {
        val block = compatibility.substringAfter(coverageStartMarker, missingDelimiterValue = "")
        assertTrue(
            block.isNotBlank(),
            "docs/protocol-compatibility.md must include $coverageStartMarker.",
        )

        val body = block.substringBefore(coverageEndMarker, missingDelimiterValue = "")
        assertTrue(
            body.isNotBlank(),
            "docs/protocol-compatibility.md must include $coverageEndMarker.",
        )
        return "$coverageStartMarker$body$coverageEndMarker"
    }

    private fun loadDocument(path: String): String {
        val start = File(System.getProperty("user.dir")).canonicalFile
        val document = generateSequence(start) { directory -> directory.parentFile }
            .map { directory -> File(directory, path) }
            .firstOrNull { file -> file.isFile }

        requireNotNull(document) {
            "Could not find $path from ${start.path} or its parent directories."
        }
        return document.readText()
    }

    private data class CoverageSummary(
        val requestGroups: Map<String, Int>,
        val notificationGroups: Map<String, Int>,
        val serverRequestGroups: Map<String, Int>,
        val modeledRequestDescriptors: Int,
    ) {
        fun toCompatibilityBlock(): String {
            return """
                $coverageStartMarker
                | Inventory section | `modeled` | `partial` | `deferred` | `experimental` | Exact current coverage |
                | --- | ---: | ---: | ---: | ---: | --- |
                | Request groups | ${requestGroups.count("modeled")} | ${requestGroups.count("partial")} | ${requestGroups.count("deferred")} | ${requestGroups.count("experimental")} | $modeledRequestDescriptors public `CodexRpc` request descriptors |
                | Notification groups | ${notificationGroups.count("modeled")} | ${notificationGroups.count("partial")} | ${notificationGroups.count("deferred")} | ${notificationGroups.count("experimental")} | Not counted by this helper |
                | Server-request groups | ${serverRequestGroups.count("modeled")} | ${serverRequestGroups.count("partial")} | ${serverRequestGroups.count("deferred")} | ${serverRequestGroups.count("experimental")} | Not counted by this helper |
                $coverageEndMarker
            """.trimIndent()
        }

        private fun Map<String, Int>.count(status: String): Int = getValue(status)
    }

    private companion object {
        private const val coverageStartMarker = "<!-- codex-rpc-coverage:start -->"
        private const val coverageEndMarker = "<!-- codex-rpc-coverage:end -->"
        private val coverageStatuses = listOf("modeled", "partial", "deferred", "experimental")
    }
}
