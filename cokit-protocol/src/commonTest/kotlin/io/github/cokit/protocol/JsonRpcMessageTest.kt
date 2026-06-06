package io.github.cokit.protocol

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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
}
