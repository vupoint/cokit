package io.github.vupoint.cokit.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable(with = JsonRpcMessageSerializer::class)
sealed interface JsonRpcMessage

@Serializable
data class JsonRpcRequest(
    val id: JsonRpcId,
    val method: String,
    val params: JsonElement? = null,
) : JsonRpcMessage

@Serializable
data class JsonRpcNotification(
    val method: String,
    val params: JsonElement? = null,
) : JsonRpcMessage

@Serializable
data class JsonRpcResponse(
    val id: JsonRpcId,
    val result: JsonElement? = null,
    val error: JsonRpcErrorObject? = null,
) : JsonRpcMessage

@Serializable(with = JsonRpcIdSerializer::class)
sealed interface JsonRpcId {
    @JvmInline
    value class Number(val value: Long) : JsonRpcId

    @JvmInline
    value class StringId(val value: String) : JsonRpcId
}
