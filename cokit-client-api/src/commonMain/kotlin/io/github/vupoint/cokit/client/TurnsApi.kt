package io.github.vupoint.cokit.client

/**
 * Turn execution helpers exposed by [CodexClient.turns].
 *
 * Request responses acknowledge server operations; observe [CodexClient.notifications] for
 * streamed items and turn completion. Cancelling a local wait does not interrupt a running turn.
 */
interface TurnsApi {
    /** Starts a turn and returns its initial snapshot before streamed execution completes. */
    suspend fun start(request: StartTurnRequest): Turn

    /** Adds input to the expected active turn and returns the server's turn ID. */
    suspend fun steer(request: SteerTurnRequest): TurnId

    /** Requests interruption of a turn; observe notifications for its eventual terminal state. */
    suspend fun interrupt(request: InterruptTurnRequest)
}
