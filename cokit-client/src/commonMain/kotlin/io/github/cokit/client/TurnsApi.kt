package io.github.cokit.client

import io.github.cokit.rpc.JsonRpcSession
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TurnsApi internal constructor(
    private val rpc: JsonRpcSession,
) {
    suspend fun start(
        threadId: String,
        input: List<JsonElement> = emptyList(),
        cwd: String? = null,
        approvalPolicy: String? = null,
        sandboxPolicy: String? = null,
        permissions: JsonElement? = null,
        model: String? = null,
        effort: String? = null,
        outputSchema: JsonElement? = null,
    ): Turn {
        val result = rpc.request(
            method = "turn/start",
            params = buildJsonObject {
                put("threadId", threadId)
                put("input", JsonArray(input))
                cwd?.let { put("cwd", it) }
                approvalPolicy?.let { put("approvalPolicy", it) }
                sandboxPolicy?.let { put("sandboxPolicy", it) }
                permissions?.let { put("permissions", it) }
                model?.let { put("model", it) }
                effort?.let { put("effort", it) }
                outputSchema?.let { put("outputSchema", it) }
            },
        )
        return result.decodeResult<TurnResult>().turn
    }

    suspend fun steer(
        threadId: String,
        expectedTurnId: String,
        input: List<JsonElement>,
        clientUserMessageId: String? = null,
    ) {
        rpc.request(
            method = "turn/steer",
            params = buildJsonObject {
                put("threadId", threadId)
                put("expectedTurnId", expectedTurnId)
                put("input", JsonArray(input))
                clientUserMessageId?.let { put("clientUserMessageId", it) }
            },
        )
    }

    suspend fun interrupt(threadId: String, turnId: String) {
        rpc.request(
            method = "turn/interrupt",
            params = buildJsonObject {
                put("threadId", threadId)
                put("turnId", turnId)
            },
        )
    }
}
