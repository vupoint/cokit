@file:OptIn(ExperimentalCodexApi::class)

package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.rpc.JsonRpcSession

internal class DefaultTurnsApi(
    private val rpc: JsonRpcSession,
    private val experimentalApi: Boolean = false,
) : TurnsApi {
    override suspend fun start(request: StartTurnRequest): Turn {
        val params = request.toRpcParams()
        params.responsesapiClientMetadataPatch.requireMetadataPatchOptIn(experimentalApi)
        return rpc.request(CodexRpc.Turn.Start, params).turn
    }

    override suspend fun steer(request: SteerTurnRequest): TurnId {
        val params = request.toRpcParams()
        params.responsesapiClientMetadataPatch.requireMetadataPatchOptIn(experimentalApi)
        return rpc.request(CodexRpc.Turn.Steer, params).turnId
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
    serviceTierForTurn = serviceTierForTurn,
    toolOutput = toolOutput,
    turnTrigger = turnTrigger,
    disabledPluginIds = disabledPluginIds,
    sandbox = sandboxPolicy,
    model = model,
    effort = effort,
    summary = summary,
    outputSchema = outputSchema,
    personality = personality,
    parentTurnId = parentTurnId,
    rootTurnId = rootTurnId,
    responsesapiClientMetadataPatch = responsesapiClientMetadataPatch,
)

private fun SteerTurnRequest.toRpcParams(): TurnSteerParams = TurnSteerParams(
    threadId = threadId,
    expectedTurnId = expectedTurnId,
    input = input,
    clientUserMessageId = clientUserMessageId,
    responsesapiClientMetadataPatch = responsesapiClientMetadataPatch,
)

private fun InterruptTurnRequest.toRpcParams(): TurnInterruptParams = TurnInterruptParams(
    threadId = threadId,
    turnId = turnId,
)

internal fun TurnMetadataPatch?.requireMetadataPatchOptIn(experimentalApi: Boolean) {
    require(this == null || experimentalApi) {
        "Turn metadata patches require InitializeCapabilities(experimentalApi = true)"
    }
}
