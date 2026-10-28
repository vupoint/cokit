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

        assertEquals("codex-cli 0.165.0", properties.required("codexVersion"))
        assertEquals(
            "360319d8dee62d87e2d59fc0c21805d79519cfed",
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
            "be44f0b100f930af47d305e0075cb2a10e80b4fb2f7dc5fe51b6a71fb80096f2",
            properties.required("stableSchemaSha256"),
        )
        assertEquals(
            "a2c0073b9b71598d435b953b163c6aa02a5bcef3bb3eedc52d4befa7e63fd44e",
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
