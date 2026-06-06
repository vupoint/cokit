package io.github.cokit.client

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TurnId(val value: String)

@Serializable
data class Turn(
    val id: String,
    val status: String,
    val items: List<JsonElement> = emptyList(),
    val error: JsonElement? = null,
)

@Serializable
internal data class TurnResult(
    val turn: Turn,
)
