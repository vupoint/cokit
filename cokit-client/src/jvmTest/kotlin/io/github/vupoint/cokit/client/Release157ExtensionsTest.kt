package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.protocol.CodexProtocolJson
import io.github.vupoint.cokit.protocol.JsonRpcNotification
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Release157ExtensionsTest {
    @Test
    fun stableReleaseExtensionDescriptorsExist() {
        val methods = CodexRpc::class.java.declaredClasses.flatMap { holder ->
            val instance = holder.getField("INSTANCE").get(null)
            holder.declaredFields.filter { it.type == CodexRpcMethod::class.java }.map {
                it.isAccessible = true
                (it.get(instance) as CodexRpcMethod<*, *>).method
            }
        }.toSet()
        val expected = setOf("thread/attachment/add", "thread/attachment/list", "thread/attachment/remove",
            "account/gatewayOAuth/read", "account/gatewayOAuth/login", "account/gatewayOAuth/cancel")
        assertTrue(methods.containsAll(expected), "Missing: ${expected - methods}")
    }

    @Test
    fun stableReleaseEventsAreTyped() {
        for ((method, params) in listOf(
            "thread/attachment/updated" to """{"threadId":"thr_1","attachmentId":"att_1","attachmentType":"pull_request","identityKey":"example","operation":"created"}""",
            "account/gatewayOAuth/changed" to """{"providerId":"example","status":"started","authUrl":"https://example.invalid/authorize"}""",
        )) {
            val notification = JsonRpcNotification(method, CodexProtocolJson.parseToJsonElement(params)).toCodexNotification()
            assertFalse(notification is CodexNotification.Unknown, method)
        }
    }
}
