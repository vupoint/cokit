package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.protocol.CodexProtocolJson
import kotlinx.serialization.json.JsonElement

internal fun CodexJsonPayload?.toJsonElement(): JsonElement? =
    this?.let { CodexProtocolJson.parseToJsonElement(it.toJsonString()) }
