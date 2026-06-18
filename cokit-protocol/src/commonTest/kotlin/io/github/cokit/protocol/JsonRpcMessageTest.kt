package io.github.cokit.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class JsonRpcMessageTest {
    private val json = Json {
        ignoreUnknownKeys = false
    }

    @Test
    fun requestOmitsJsonRpcHeaderOnWire() {
        val encoded = json.encodeToString(
            JsonRpcRequest.serializer(),
            JsonRpcRequest(id = JsonRpcId.Number(1), method = "initialize", params = null),
        )

        assertFalse(encoded.contains("jsonrpc"))
        assertTrue(encoded.contains("initialize"))
    }

    @Test
    fun decodesNotificationWithoutId() {
        val decoded = json.decodeFromString<JsonRpcMessage>(
            """{"method":"thread/started","params":{"thread":{"id":"thr_123"}}}""",
        )

        assertTrue(decoded is JsonRpcNotification)
    }

    @Test
    fun decodesStringIdAsStringEvenWhenContentIsNumeric() {
        val decoded = json.decodeFromString<JsonRpcMessage>(
            """{"id":"123","result":{"ok":true}}""",
        )

        assertTrue(decoded is JsonRpcResponse)
        assertEquals(JsonRpcId.StringId("123"), decoded.id)
    }

    @Test
    fun decodesNumericIdAsNumber() {
        val decoded = json.decodeFromString<JsonRpcMessage>(
            """{"id":123,"result":{"ok":true}}""",
        )

        assertTrue(decoded is JsonRpcResponse)
        assertEquals(JsonRpcId.Number(123), decoded.id)
    }

    @Test
    fun encodesStringAndNumericIdsWithTheirOriginalJsonKinds() {
        val stringId = json.encodeToString(
            JsonRpcResponse.serializer(),
            JsonRpcResponse(id = JsonRpcId.StringId("123")),
        )
        val numericId = json.encodeToString(
            JsonRpcResponse.serializer(),
            JsonRpcResponse(id = JsonRpcId.Number(123)),
        )

        assertTrue(stringId.contains(""""id":"123""""))
        assertTrue(numericId.contains(""""id":123"""))
    }

    @Test
    fun rejectsMalformedRequestWithNonStringMethod() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<JsonRpcMessage>(
                """{"id":1,"method":7,"params":{}}""",
            )
        }
    }

    @Test
    fun rejectsMalformedResponseWithBothResultAndError() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<JsonRpcMessage>(
                """{"id":1,"result":{"ok":true},"error":{"code":-32000,"message":"failed"}}""",
            )
        }
    }

    @Test
    fun rejectsMalformedNotificationWithNonStringMethod() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<JsonRpcMessage>(
                """{"method":7,"params":{}}""",
            )
        }
    }

    @Test
    fun rejectsResultMessageMissingId() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<JsonRpcMessage>(
                """{"result":{"ok":true}}""",
            )
        }
    }

    @Test
    fun rejectsBadIdType() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<JsonRpcMessage>(
                """{"id":{"value":1},"result":{"ok":true}}""",
            )
        }
    }
}
