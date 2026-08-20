package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.rpc.JsonRpcSession

internal class DefaultTurnsApi(
    private val rpc: JsonRpcSession,
) : TurnsApi {
    override suspend fun start(request: StartTurnRequest): Turn {
        return rpc.request(CodexRpc.Turn.Start, request.toRpcParams()).turn
    }

    override suspend fun steer(request: SteerTurnRequest): TurnId {
        return rpc.request(CodexRpc.Turn.Steer, request.toRpcParams()).turnId
    }

    override suspend fun interrupt(request: InterruptTurnRequest) {
        rpc.request(CodexRpc.Turn.Interrupt, request.toRpcParams())
    }
}

private fun StartTurnRequest.toRpcParams(): TurnStartParams = TurnStartParams(
    threadId = threadId,
    input = input,
    cwd = cwd,
    approvalPolicy = approvalPolicy,
    approvalsReviewer = approvalsReviewer,
    clientUserMessageId = clientUserMessageId,
    serviceTier = serviceTier,
    sandbox = sandboxPolicy,
    model = model,
    effort = effort,
    summary = summary,
    outputSchema = outputSchema,
    personality = personality,
)

private fun SteerTurnRequest.toRpcParams(): TurnSteerParams = TurnSteerParams(
    threadId = threadId,
    expectedTurnId = expectedTurnId,
    input = input,
    clientUserMessageId = clientUserMessageId,
)

private fun InterruptTurnRequest.toRpcParams(): TurnInterruptParams = TurnInterruptParams(
    threadId = threadId,
    turnId = turnId,
)
