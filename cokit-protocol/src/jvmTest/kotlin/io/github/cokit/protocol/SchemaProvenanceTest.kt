package io.github.cokit.protocol

import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SchemaProvenanceTest {
    @Test
    fun schemaProvenanceRecordsRequiredAuditFields() {
        val properties = loadSchemaProvenance()

        assertTrue(properties.required("codexVersion").isNotBlank())
        assertTrue(Regex("[0-9a-f]{40}").matches(properties.required("upstreamCommit")))
        assertEquals(
            "codex app-server generate-json-schema --out build/generated/codex-schema/stable",
            properties.required("stableCommand"),
        )
        assertEquals(
            "codex app-server generate-json-schema --out build/generated/codex-schema/experimental --experimental",
            properties.required("experimentalCommand"),
        )
        assertTrue(Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{2}:\d{2}""").matches(properties.required("generatedAt")))
    }

    private fun loadSchemaProvenance(): Properties {
        val resource = SchemaProvenanceTest::class.java.classLoader
            .getResourceAsStream("codex-schema-provenance.properties")

        assertNotNull(resource, "codex-schema-provenance.properties should be packaged as a protocol resource.")
        return resource.use { stream ->
            Properties().also { properties -> properties.load(stream) }
        }
    }

    private fun Properties.required(key: String): String {
        val value = getProperty(key)
        assertNotNull(value, "$key should be recorded in codex-schema-provenance.properties.")
        return value
    }
}
