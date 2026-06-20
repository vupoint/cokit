package io.github.vupoint.cokit.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class JsonRpcErrorObject(
    val code: Int,
    val message: String,
    val data: JsonElement? = null,
)
