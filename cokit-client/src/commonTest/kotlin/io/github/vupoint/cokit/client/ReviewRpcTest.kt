package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.review.ReviewDelivery
import io.github.vupoint.cokit.client.review.ReviewStartParams
import io.github.vupoint.cokit.client.review.ReviewTarget
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReviewRpcTest {
    @Test
    fun reviewStartDescriptorSendsTypedTargetAndDecodesReviewThread() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val result = async {
            fixture.client.request(
                CodexRpc.Review.Start,
                ReviewStartParams(
                    threadId = ThreadId("thr_main"),
                    delivery = ReviewDelivery.Detached,
                    target = ReviewTarget.Commit(
                        sha = "abc123",
                        title = "Add review descriptor",
                    ),
                ),
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("review/start", request.method)
        val params = request.params!!.jsonObject
        assertEquals("thr_main", params["threadId"]?.jsonPrimitive?.contentOrNull)
        assertEquals("detached", params["delivery"]?.jsonPrimitive?.contentOrNull)
        val target = params["target"]!!.jsonObject
        assertEquals("commit", target["type"]?.jsonPrimitive?.contentOrNull)
        assertEquals("abc123", target["sha"]?.jsonPrimitive?.contentOrNull)
        assertEquals("Add review descriptor", target["title"]?.jsonPrimitive?.contentOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put("reviewThreadId", "thr_review")
                    put(
                        "turn",
                        buildJsonObject {
                            put("id", "turn_review")
                            put("status", "inProgress")
                            put("items", buildJsonArray {})
                        },
                    )
                },
            ),
        )

        val decoded = result.await()
        assertEquals(ThreadId("thr_review"), decoded.reviewThreadId)
        assertEquals(TurnId("turn_review"), decoded.turn.id)
        assertEquals(TurnStatus.InProgress, decoded.turn.status)
    }

    private suspend fun TestScope.connectedRpcClientFixture(
        scope: CoroutineScope,
    ): ConnectedRpcClientFixture {
        val transport = FakeJsonRpcTransport()
        val client = async {
            CodexClients.connect(
                CodexClientConnection(
                    transport = transport,
                    clientInfo = ClientInfo("cokit_test", "CoKit Test", "0.1.0"),
                    scope = scope,
                ),
            )
        }
        runCurrent()
        val initialize = transport.sent.single() as JsonRpcRequest
        transport.receive(JsonRpcResponse(initialize.id, result = JsonObject(emptyMap())))
        return ConnectedRpcClientFixture(client.await(), transport)
    }

    private data class ConnectedRpcClientFixture(
        val client: CodexClient,
        val transport: FakeJsonRpcTransport,
    )
}
