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

        assertEquals("codex-cli 0.146.0", properties.required("codexVersion"))
        assertEquals(
            "e363b08c9175ac1cbe5893615dd2cb9ddf95043b",
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
            "2f402b7d1356adccc1a4785c0656db457578ca9ea5d5b08953487a410c630ce8",
            properties.required("stableSchemaSha256"),
        )
        assertEquals(
            "2453c0d4a58820f40e4d9dddcafdb92d37d5f7f5eb7606a684f5380ad2cddcd8",
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
