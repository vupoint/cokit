package io.github.cokit.client

import io.github.cokit.protocol.CodexProtocolJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ThreadTurnModelTest {
    @Test
    fun serializesThreadTurnAndItemScalarModelsAsProtocolPrimitives() {
        assertStringScalar("item_123", ItemId.serializer(), ItemId("item_123"))
        assertStringScalar("cursor_123", CodexCursor.serializer(), CodexCursor("cursor_123"))
        assertStringScalar("msg_123", ClientMessageId.serializer(), ClientMessageId("msg_123"))
        assertStringScalar("notLoaded", ThreadStatusType.serializer(), ThreadStatusType.NotLoaded)
        assertStringScalar("inProgress", TurnStatus.serializer(), TurnStatus.InProgress)
        assertStringScalar("completed", ItemStatus.serializer(), ItemStatus.Completed)

        val encoded = CodexProtocolJson.encodeToString(
            CodexTimestamp.serializer(),
            CodexTimestamp(1730910000),
        )
        assertEquals("1730910000", encoded)
        assertEquals(
            CodexTimestamp(1730910000),
            CodexProtocolJson.decodeFromString(encoded),
        )
    }

    @Test
    fun threadModelsUseTypedScalarsWithoutChangingWireShape() {
        val encoded = CodexProtocolJson.encodeToJsonElement(
            Thread.serializer(),
            Thread(
                id = ThreadId("thr_123"),
                preview = "Fix tests",
                modelProvider = "openai",
                createdAt = CodexTimestamp(1730910000),
            ),
        ).jsonObject

        assertEquals("thr_123", encoded["id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("Fix tests", encoded["preview"]?.jsonPrimitive?.contentOrNull)
        assertEquals("openai", encoded["modelProvider"]?.jsonPrimitive?.contentOrNull)
        assertEquals(
            1730910000,
            CodexProtocolJson.decodeFromJsonElement<Int>(encoded.getValue("createdAt")),
        )

        val listParams = CodexProtocolJson.encodeToJsonElement(
            ThreadListParams.serializer(),
            ThreadListParams(cursor = CodexCursor("cursor_123")),
        ).jsonObject

        assertEquals("cursor_123", listParams["cursor"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun turnModelsUseTypedScalarsWithoutChangingWireShape() {
        val turn = CodexProtocolJson.decodeFromString<Turn>(
            """{"id":"turn_123","status":"inProgress","items":[]}""",
        )

        assertEquals(TurnId("turn_123"), turn.id)
        assertEquals(TurnStatus.InProgress, turn.status)

        val steerParams = CodexProtocolJson.encodeToJsonElement(
            TurnSteerParams.serializer(),
            TurnSteerParams(
                threadId = ThreadId("thr_123"),
                expectedTurnId = TurnId("turn_123"),
                input = listOf(TurnInput.Text("Keep going")),
                clientUserMessageId = ClientMessageId("msg_123"),
            ),
        ).jsonObject

        assertEquals("msg_123", steerParams["clientUserMessageId"]?.jsonPrimitive?.contentOrNull)
    }

    private fun <T> assertStringScalar(
        expected: String,
        serializer: KSerializer<T>,
        value: T,
    ) {
        val encoded = CodexProtocolJson.encodeToString(serializer, value)

        assertEquals(CodexProtocolJson.encodeToString(String.serializer(), expected), encoded)
        assertEquals(value, CodexProtocolJson.decodeFromString(serializer, encoded))
    }
}
