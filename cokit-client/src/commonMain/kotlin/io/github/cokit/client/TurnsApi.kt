package io.github.cokit.client

import io.github.cokit.protocol.CodexProtocolJson
import io.github.cokit.rpc.JsonRpcSession
import kotlinx.serialization.json.encodeToJsonElement

class TurnsApi internal constructor(
    private val rpc: JsonRpcSession,
) {
    suspend fun start(request: StartTurnRequest): Turn {
        val result = rpc.request(
            method = "turn/start",
            params = CodexProtocolJson.encodeToJsonElement(StartTurnRequest.serializer(), request),
        )
        return result.decodeResult<TurnResult>().turn
    }

    suspend fun steer(request: SteerTurnRequest) {
        rpc.request(
            method = "turn/steer",
            params = CodexProtocolJson.encodeToJsonElement(SteerTurnRequest.serializer(), request),
        )
    }

    suspend fun interrupt(request: InterruptTurnRequest) {
        rpc.request(
            method = "turn/interrupt",
            params = CodexProtocolJson.encodeToJsonElement(InterruptTurnRequest.serializer(), request),
        )
    }
}
