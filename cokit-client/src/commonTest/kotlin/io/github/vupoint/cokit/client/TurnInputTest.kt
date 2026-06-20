package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.protocol.CodexProtocolJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TurnInputTest {
    @Test
    fun serializesKnownTurnInputVariants() {
        assertEncoded(
            TurnInput.Text("Run tests"),
            "type" to "text",
            "text" to "Run tests",
        )
        assertEncoded(
            TurnInput.Image("https://example.com/screenshot.png"),
            "type" to "image",
            "url" to "https://example.com/screenshot.png",
        )
        assertEncoded(
            TurnInput.LocalImage("/tmp/screenshot.png"),
            "type" to "localImage",
            "path" to "/tmp/screenshot.png",
        )
        assertEncoded(
            TurnInput.Skill(
                name = "skill-creator",
                path = "/path/to/skills/skill-creator/SKILL.md",
            ),
            "type" to "skill",
            "name" to "skill-creator",
            "path" to "/path/to/skills/skill-creator/SKILL.md",
        )
        assertEncoded(
            TurnInput.Mention(
                name = "Demo App",
                path = "app://demo-app",
            ),
            "type" to "mention",
            "name" to "Demo App",
            "path" to "app://demo-app",
        )
    }

    @Test
    fun deserializesUnknownTurnInputAsCustomPayload() {
        val element = CodexProtocolJson.decodeFromString<JsonObject>(
            """{"type":"futureInput","value":"kept"}""",
        )

        val input = CodexProtocolJson.decodeFromJsonElement<TurnInput>(element)

        val custom = assertIs<TurnInput.Custom>(input)
        assertEquals("""{"type":"futureInput","value":"kept"}""", custom.payload.toJsonString())
    }

    @Test
    fun customTurnInputPreservesOriginalJson() {
        val element = CodexProtocolJson.decodeFromString<JsonObject>(
            """{"type":"futureInput","value":"kept"}""",
        )

        val encoded = CodexProtocolJson.encodeToJsonElement(
            TurnInput.serializer(),
            TurnInput.Custom(
                CodexJsonPayload.parse("""{"type":"futureInput","value":"kept"}"""),
            ),
        )

        assertEquals(element, encoded)
    }

    private fun assertEncoded(
        input: TurnInput,
        vararg fields: Pair<String, String>,
    ) {
        val obj = CodexProtocolJson.encodeToJsonElement(TurnInput.serializer(), input).jsonObject

        fields.forEach { (name, value) ->
            assertEquals(value, obj[name]?.jsonPrimitive?.contentOrNull)
        }
    }
}
