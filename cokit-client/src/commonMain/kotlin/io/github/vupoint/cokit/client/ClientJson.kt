package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.protocol.CodexProtocolJson
import kotlinx.serialization.json.JsonElement

internal fun JsonElement.toCodexPayload(): CodexJsonPayload =
    CodexJsonPayload.parse(CodexProtocolJson.encodeToString(JsonElement.serializer(), this))

internal fun CodexJsonPayload?.toJsonElement(): JsonElement? =
    this?.let { CodexProtocolJson.parseToJsonElement(it.toJsonString()) }
