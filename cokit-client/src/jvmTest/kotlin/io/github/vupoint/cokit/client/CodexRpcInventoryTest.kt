package io.github.vupoint.cokit.client

import java.io.File
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodexRpcInventoryTest {
    @Test
    fun publicInventoryListsEveryCodexRpcDescriptor() {
        val descriptorMethods = codexRpcDescriptorMethods()
        val inventoryMethods = inventoryDescriptorMethods(loadProtocolInventory())

        assertEquals(
            descriptorMethods,
            inventoryMethods,
            "docs/protocol-inventory.md must list every public CodexRpc descriptor method.",
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

    private fun loadProtocolInventory(): String {
        val start = File(System.getProperty("user.dir")).canonicalFile
        val inventory = generateSequence(start) { directory -> directory.parentFile }
            .map { directory -> File(directory, "docs/protocol-inventory.md") }
            .firstOrNull { file -> file.isFile }

        requireNotNull(inventory) {
            "Could not find docs/protocol-inventory.md from ${start.path} or its parent directories."
        }
        return inventory.readText()
    }

    private fun inventoryDescriptorMethods(inventory: String): Set<String> {
        val sectionTitle = "## Current Modeled Request Descriptors"
        val section = inventory.substringAfter(sectionTitle, missingDelimiterValue = "")
        assertTrue(
            section.isNotBlank(),
            "$sectionTitle section is required for CodexRpc descriptor coverage.",
        )

        val table = section.substringBefore("\n## ")
        return Regex("""^\|\s*`CodexRpc\.[^`]+`\s*\|\s*`([^`]+)`\s*\|""", RegexOption.MULTILINE)
            .findAll(table)
            .map { match -> match.groupValues[1] }
            .toSortedSet()
    }
}
