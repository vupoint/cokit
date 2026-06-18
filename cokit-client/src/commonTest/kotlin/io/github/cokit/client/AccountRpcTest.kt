package io.github.cokit.client

import io.github.cokit.client.auth.AccountEmail
import io.github.cokit.client.auth.AccountPlanType
import io.github.cokit.client.auth.AccountRateLimitReachedType
import io.github.cokit.client.auth.AccountRateLimitStatus
import io.github.cokit.client.auth.AccountRateLimitWindow
import io.github.cokit.client.auth.AccountRateLimitsReadParams
import io.github.cokit.client.auth.AccountUsageReadParams
import io.github.cokit.client.auth.AddCreditsNudgeCreditType
import io.github.cokit.client.auth.AddCreditsNudgeEmailStatus
import io.github.cokit.client.auth.AccountReadParams
import io.github.cokit.client.auth.CancelLoginAccountParams
import io.github.cokit.client.auth.CancelLoginAccountResult
import io.github.cokit.client.auth.CancelLoginAccountStatus
import io.github.cokit.client.auth.CodexAccount
import io.github.cokit.client.auth.LoginAccountId
import io.github.cokit.client.auth.LoginAccountParams
import io.github.cokit.client.auth.LoginAccountResult
import io.github.cokit.client.auth.LogoutAccountParams
import io.github.cokit.client.auth.SendAddCreditsNudgeEmailParams
import io.github.cokit.client.auth.SendAddCreditsNudgeEmailResult
import io.github.cokit.protocol.JsonRpcNotification
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
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, ExperimentalCodexApi::class)
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

    @Test
    fun accountLoginStartEncodesBrowserFlowAndRedactsAuthUrl() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val loginResult = async {
            fixture.client.request(
                CodexRpc.Account.StartLogin,
                LoginAccountParams.ChatGpt(codexStreamlinedLogin = true),
            )
        }
        runCurrent()

        val login = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("account/login/start", login.method)
        val loginParams = login.params!!.jsonObject
        assertEquals("chatgpt", loginParams["type"]?.jsonPrimitive?.contentOrNull)
        assertEquals(true, loginParams["codexStreamlinedLogin"]?.jsonPrimitive?.booleanOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                login.id,
                result = buildJsonObject {
                    put("type", "chatgpt")
                    put("loginId", "login_browser")
                    put("authUrl", "https://auth.example.invalid/browser")
                },
            ),
        )

        val browserLogin = assertIs<LoginAccountResult.ChatGpt>(loginResult.await())
        assertEquals(LoginAccountId("login_browser"), browserLogin.loginId)
        assertEquals("https://auth.example.invalid/browser", browserLogin.authUrl)
        assertFalse(browserLogin.toString().contains("https://auth.example.invalid/browser"))
    }

    @Test
    fun accountLoginStartEncodesDeviceCodeFlowAndRedactsUserCode() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val loginResult = async {
            fixture.client.request(
                CodexRpc.Account.StartLogin,
                LoginAccountParams.ChatGptDeviceCode,
            )
        }
        runCurrent()

        val login = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("account/login/start", login.method)
        assertEquals("chatgptDeviceCode", login.params!!.jsonObject["type"]?.jsonPrimitive?.contentOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                login.id,
                result = buildJsonObject {
                    put("type", "chatgptDeviceCode")
                    put("loginId", "login_device")
                    put("verificationUrl", "https://auth.example.invalid/device")
                    put("userCode", "CODE-1234")
                },
            ),
        )

        val deviceLogin = assertIs<LoginAccountResult.ChatGptDeviceCode>(loginResult.await())
        assertEquals(LoginAccountId("login_device"), deviceLogin.loginId)
        assertEquals("https://auth.example.invalid/device", deviceLogin.verificationUrl)
        assertEquals("CODE-1234", deviceLogin.userCode)
        assertFalse(deviceLogin.toString().contains("https://auth.example.invalid/device"))
        assertFalse(deviceLogin.toString().contains("CODE-1234"))
    }

    @Test
    fun accountApiKeyLoginAndExternalTokenLoginDoNotLeakSecretsInStringRepresentations() {
        val apiKey = LoginAccountParams.ApiKey(apiKey = "test-api-key")
        assertFalse(apiKey.toString().contains("test-api-key"))

        val externalTokens = LoginAccountParams.ChatGptAuthTokens(
            accessToken = "test-access-token",
            chatgptAccountId = "acct_test",
            chatgptPlanType = AccountPlanType.Plus,
        )
        assertFalse(externalTokens.toString().contains("test-access-token"))
        assertFalse(externalTokens.toString().contains("acct_test"))
    }

    @Test
    fun accountLoginCancelSendsLoginIdAndDecodesCancelStatus() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val cancelResult = async {
            fixture.client.request(
                CodexRpc.Account.CancelLogin,
                CancelLoginAccountParams(LoginAccountId("login_browser")),
            )
        }
        runCurrent()

        val cancel = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("account/login/cancel", cancel.method)
        assertEquals("login_browser", cancel.params!!.jsonObject["loginId"]?.jsonPrimitive?.contentOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                cancel.id,
                result = buildJsonObject {
                    put("status", "canceled")
                },
            ),
        )

        assertEquals(
            CancelLoginAccountResult(CancelLoginAccountStatus.Canceled),
            cancelResult.await(),
        )
    }

    @Test
    fun decodesAccountLoginCompletedNotifications() {
        val success = JsonRpcNotification(
            method = "account/login/completed",
            params = buildJsonObject {
                put("loginId", "login_browser")
                put("success", true)
                put("error", null)
            },
        ).toCodexNotification()

        val completed = assertIs<CodexNotification.AccountLoginCompleted>(success)
        assertEquals(LoginAccountId("login_browser"), completed.loginId)
        assertEquals(true, completed.success)
        assertNull(completed.error)

        val failure = JsonRpcNotification(
            method = "account/login/completed",
            params = buildJsonObject {
                put("loginId", null)
                put("success", false)
                put("error", "Login was canceled.")
            },
        ).toCodexNotification()

        val failed = assertIs<CodexNotification.AccountLoginCompleted>(failure)
        assertNull(failed.loginId)
        assertEquals(false, failed.success)
        assertEquals("Login was canceled.", failed.error)
    }

    @Test
    fun accountRateLimitsDescriptorSendsNoParamsAndDecodesSnapshots() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val rateLimitsResult = async {
            fixture.client.request(CodexRpc.Account.ReadRateLimits, AccountRateLimitsReadParams)
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("account/rateLimits/read", request.method)
        assertNull(request.params)

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put("rateLimits", rateLimitSnapshot())
                    put(
                        "rateLimitsByLimitId",
                        buildJsonObject {
                            put("codex", rateLimitSnapshot(limitId = "codex"))
                        },
                    )
                },
            ),
        )

        val decoded = rateLimitsResult.await()
        assertEquals(42, decoded.rateLimits.primary?.usedPercent)
        assertEquals(1_800L, decoded.rateLimits.primary?.windowDurationMins)
        assertEquals(true, decoded.rateLimits.credits?.hasCredits)
        assertEquals(AccountPlanType.Team, decoded.rateLimits.planType)
        assertEquals(AccountRateLimitReachedType.WorkspaceMemberUsageLimitReached, decoded.rateLimits.rateLimitReachedType)
        assertEquals(8, decoded.rateLimitsByLimitId?.get("codex")?.secondary?.usedPercent)
    }

    @Test
    fun accountUsageDescriptorSendsNoParamsAndDecodesSummaryAndDailyBuckets() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val usageResult = async {
            fixture.client.request(CodexRpc.Account.ReadUsage, AccountUsageReadParams)
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("account/usage/read", request.method)
        assertNull(request.params)

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put(
                        "summary",
                        buildJsonObject {
                            put("lifetimeTokens", 1_000_000)
                            put("peakDailyTokens", 50_000)
                            put("currentStreakDays", 5)
                            put("longestStreakDays", 9)
                            put("longestRunningTurnSec", 120)
                        },
                    )
                    put(
                        "dailyUsageBuckets",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("startDate", "2026-06-18")
                                    put("tokens", 25_000)
                                },
                            )
                        },
                    )
                },
            ),
        )

        val decoded = usageResult.await()
        assertEquals(1_000_000L, decoded.summary.lifetimeTokens)
        assertEquals(50_000L, decoded.summary.peakDailyTokens)
        assertEquals("2026-06-18", decoded.dailyUsageBuckets?.single()?.startDate)
        assertEquals(25_000L, decoded.dailyUsageBuckets?.single()?.tokens)
    }

    @Test
    fun accountAddCreditsNudgeDescriptorSendsCreditTypeAndDecodesStatus() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val addCreditsResult = async {
            fixture.client.request(
                CodexRpc.Account.SendAddCreditsNudgeEmail,
                SendAddCreditsNudgeEmailParams(AddCreditsNudgeCreditType.UsageLimit),
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("account/sendAddCreditsNudgeEmail", request.method)
        assertEquals("usage_limit", request.params!!.jsonObject["creditType"]?.jsonPrimitive?.contentOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put("status", "cooldown_active")
                },
            ),
        )

        assertEquals(
            SendAddCreditsNudgeEmailResult(AddCreditsNudgeEmailStatus.CooldownActive),
            addCreditsResult.await(),
        )
    }

    @Test
    fun decodesAccountRateLimitsUpdatedNotifications() {
        val notification = JsonRpcNotification(
            method = "account/rateLimits/updated",
            params = buildJsonObject {
                put("rateLimits", rateLimitSnapshot(limitId = "codex"))
            },
        ).toCodexNotification()

        val updated = assertIs<CodexNotification.AccountRateLimitsUpdated>(notification)
        assertEquals(AccountRateLimitWindow(usedPercent = 42, resetsAt = 1_900_000L, windowDurationMins = 1_800L), updated.rateLimits.primary)
        assertEquals(AccountRateLimitStatus(hasCredits = true, unlimited = false, balance = "25.00"), updated.rateLimits.credits)
        assertEquals("codex", updated.rateLimits.limitId)
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

    private fun rateLimitSnapshot(limitId: String? = null): JsonObject =
        buildJsonObject {
            put("limitId", limitId)
            put("limitName", "Codex")
            put("planType", "team")
            put("rateLimitReachedType", "workspace_member_usage_limit_reached")
            put(
                "primary",
                buildJsonObject {
                    put("usedPercent", 42)
                    put("resetsAt", 1_900_000)
                    put("windowDurationMins", 1_800)
                },
            )
            put(
                "secondary",
                buildJsonObject {
                    put("usedPercent", 8)
                },
            )
            put(
                "credits",
                buildJsonObject {
                    put("hasCredits", true)
                    put("unlimited", false)
                    put("balance", "25.00")
                },
            )
            put(
                "individualLimit",
                buildJsonObject {
                    put("limit", "100.00")
                    put("used", "42.00")
                    put("remainingPercent", 58)
                    put("resetsAt", 1_900_000)
                },
            )
        }
}
