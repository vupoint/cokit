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

        assertEquals("codex-cli 0.157.1", properties.required("codexVersion"))
        assertEquals(
            "36650394c5b38c2990ccf2a3457165ca3e9d9726",
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
            "8eb52e0f1d39c5b2263753bfadbbccb74032eab0639b8ca79eb5349e16ac8d73",
            properties.required("stableSchemaSha256"),
        )
        assertEquals(
            "ff9bcc67a07f763a9e61954019c652937ae96b1f7ca2319f2ad23d9cb567c571",
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
