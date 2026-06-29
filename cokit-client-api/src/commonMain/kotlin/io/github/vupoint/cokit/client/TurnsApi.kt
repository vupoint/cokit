package io.github.vupoint.cokit.client

interface TurnsApi {
    suspend fun start(request: StartTurnRequest): Turn

    suspend fun steer(request: SteerTurnRequest)

    suspend fun interrupt(request: InterruptTurnRequest)
}
