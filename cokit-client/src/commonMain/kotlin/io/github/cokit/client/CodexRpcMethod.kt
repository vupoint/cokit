package io.github.cokit.client

import kotlinx.serialization.KSerializer

class CodexRpcMethod<P : Any, R : Any> internal constructor(
    val method: String,
    internal val paramsSerializer: KSerializer<P>,
    internal val resultSerializer: KSerializer<R>,
)

object CodexRpc {
    object Thread {
        val Start: CodexRpcMethod<ThreadStartParams, ThreadStartResult> = CodexRpcMethod(
            method = "thread/start",
            paramsSerializer = ThreadStartParams.serializer(),
            resultSerializer = ThreadStartResult.serializer(),
        )
    }

    object Turn {
        val Start: CodexRpcMethod<TurnStartParams, TurnStartResult> = CodexRpcMethod(
            method = "turn/start",
            paramsSerializer = TurnStartParams.serializer(),
            resultSerializer = TurnStartResult.serializer(),
        )
    }
}
