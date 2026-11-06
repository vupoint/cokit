@file:OptIn(ExperimentalCodexApi::class)

package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.protocol.CodexProtocolJson
import io.github.vupoint.cokit.protocol.JsonRpcId
import io.github.vupoint.cokit.rpc.JsonRpcSession
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal class ClientRequestExecutor(
    private val rpc: JsonRpcSession,
    private val experimentalApi: Boolean,
) {
    suspend fun <P : Any, R : Any> request(
        method: CodexRpcMethod<P, R>,
        params: P,
        onRequestId: (CodexRequestId) -> Unit = {},
    ): R {
        require(!method.requiresExperimentalApi || experimentalApi) {
            "${method.method} requires InitializeCapabilities(experimentalApi = true)"
        }
        if (params is ThreadStartParams) params.requireDynamicToolsOptIn(experimentalApi)
        if (params is ThreadForkParams) params.requireExperimentalForkOptIn(experimentalApi)
        if (params is ThreadListParams) params.requireAttachedThreadsOptIn(experimentalApi)
        if (params is TurnStartParams) params.responsesapiClientMetadataPatch.requireMetadataPatchOptIn(experimentalApi)
        if (params is TurnSteerParams) params.responsesapiClientMetadataPatch.requireMetadataPatchOptIn(experimentalApi)

        val requestParams = method.paramsSerializer?.let { serializer ->
            CodexProtocolJson.encodeToJsonElement(serializer, params)
        }
        val result = rpc.request(
            method = method.method,
            params = requestParams,
            onRequestId = { id ->
                onRequestId(when (id) {
                    is JsonRpcId.Number -> CodexRequestId.Number(id.value)
                    is JsonRpcId.StringId -> CodexRequestId.StringId(id.value)
                })
            },
        )
        return result.decodeResult(method)
    }
}

private fun <T : Any> JsonElement?.decodeResult(
    method: CodexRpcMethod<*, T>,
): T {
    if (this == null) {
        return requireNotNull(method.emptyResult) {
            "Expected a JSON-RPC result payload"
        }
    }
    return CodexProtocolJson.decodeFromJsonElement(method.resultSerializer, this)
}

private fun ThreadForkParams.requireExperimentalForkOptIn(experimentalApi: Boolean) {
    require((experimentalPredictionMode == null && attachToSource == null && experimentalDisablePredictionMcp == null) || experimentalApi) {
        "Experimental fork options require InitializeCapabilities(experimentalApi = true)"
    }
}

private fun ThreadListParams.requireAttachedThreadsOptIn(experimentalApi: Boolean) {
    require(attachedToThreadId == null || experimentalApi) {
        "Attached thread filters require InitializeCapabilities(experimentalApi = true)"
    }
}

private fun TurnMetadataPatch?.requireMetadataPatchOptIn(experimentalApi: Boolean) {
    require(this == null || experimentalApi) {
        "Turn metadata patches require InitializeCapabilities(experimentalApi = true)"
    }
}

private fun ThreadStartParams.requireDynamicToolsOptIn(experimentalApi: Boolean) {
    require(dynamicTools == null || experimentalApi) {
        "Dynamic tools require InitializeCapabilities(experimentalApi = true)"
    }
}
