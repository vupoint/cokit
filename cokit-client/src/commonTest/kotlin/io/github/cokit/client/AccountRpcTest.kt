package io.github.cokit.client

import io.github.cokit.client.auth.AccountEmail
import io.github.cokit.client.auth.AccountPlanType
import io.github.cokit.client.auth.AccountReadParams
import io.github.cokit.client.auth.CodexAccount
import io.github.cokit.client.auth.LogoutAccountParams
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import io.github.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AccountRpcTest {
    @Test
    fun accountReadDescriptorSendsRefreshFlagAndDecodesChatGptAccountWithoutLoggingIdentifier() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val result = async {
            fixture.client.request(
                CodexRpc.Account.Read,
                AccountReadParams(refreshToken = true),
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("account/read", request.method)
        assertEquals(true, request.params!!.jsonObject["refreshToken"]?.jsonPrimitive?.booleanOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put("requiresOpenaiAuth", false)
                    put(
                        "account",
                        buildJsonObject {
                            put("type", "chatgpt")
                            put("email", "user@example.invalid")
                            put("planType", "team")
                        },
                    )
                },
            ),
        )

        val decoded = result.await()
        assertEquals(false, decoded.requiresOpenaiAuth)
        val account = assertIs<CodexAccount.ChatGpt>(decoded.account)
        assertEquals(AccountEmail("user@example.invalid"), account.email)
        assertEquals(AccountPlanType.Team, account.planType)
        assertFalse(account.toString().contains("user@example.invalid"))
        assertFalse(decoded.toString().contains("user@example.invalid"))
    }

    @Test
    fun accountReadCanDecodeMissingAccountAndLogoutSendsNoParams() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val readResult = async {
            fixture.client.request(
                CodexRpc.Account.Read,
                AccountReadParams(),
            )
        }
        runCurrent()

        val read = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("account/read", read.method)
        assertEquals(JsonObject(emptyMap()), read.params)
        fixture.transport.receive(
            JsonRpcResponse(
                read.id,
                result = buildJsonObject {
                    put("requiresOpenaiAuth", true)
                    put("account", null)
                },
            ),
        )
        assertNull(readResult.await().account)

        val logoutResult = async {
            fixture.client.request(CodexRpc.Account.Logout, LogoutAccountParams)
        }
        runCurrent()

        val logout = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("account/logout", logout.method)
        assertNull(logout.params)
        fixture.transport.receive(JsonRpcResponse(logout.id, result = JsonObject(emptyMap())))
        assertEquals(CodexRpcUnit, logoutResult.await())
    }

    private suspend fun TestScope.connectedRpcClientFixture(
        scope: CoroutineScope,
    ): ConnectedRpcClientFixture {
        val transport = FakeJsonRpcTransport()
        val client = async {
            CodexRpcClient.connect(
                CodexRpcConnection(
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
        val client: CodexRpcClient,
        val transport: FakeJsonRpcTransport,
    )
}
