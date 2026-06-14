package io.github.cokit.client

import io.github.cokit.protocol.CodexProtocolJson
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

internal inline fun <reified T> JsonElement?.decodeResult(): T {
    val element = requireNotNull(this) { "Expected a JSON-RPC result payload" }
    return CodexProtocolJson.decodeFromJsonElement(element)
}

internal fun <T> JsonElement?.decodeResult(
    deserializer: DeserializationStrategy<T>,
): T {
    val element = requireNotNull(this) { "Expected a JSON-RPC result payload" }
    return CodexProtocolJson.decodeFromJsonElement(deserializer, element)
}
