package io.github.vupoint.cokit.protocol

import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SchemaProvenanceTest {
    @Test
    fun schemaProvenanceRecordsRequiredAuditFields() {
        val properties = loadSchemaProvenance()

        assertEquals("codex-cli 0.167.0", properties.required("codexVersion"))
        assertEquals(
            "086ccc833bc9cf01024ad2dd6af7dbd421b6bf9a",
            properties.required("upstreamCommit"),
        )
        assertEquals(
            "codex app-server generate-json-schema --out build/generated/codex-schema/stable",
            properties.required("stableCommand"),
        )
        assertEquals(
            "codex app-server generate-json-schema --out build/generated/codex-schema/experimental --experimental",
            properties.required("experimentalCommand"),
        )
        assertEquals(
            "2da4f6a25833beab31dafcd455a7150860f986aaaac5f40e900ba4616311d823",
            properties.required("stableSchemaSha256"),
        )
        assertEquals(
            "be0242ebc5073532edc20ba394b73c4c144f905178c361bfd6d3245dd1d31497",
            properties.required("experimentalSchemaSha256"),
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
