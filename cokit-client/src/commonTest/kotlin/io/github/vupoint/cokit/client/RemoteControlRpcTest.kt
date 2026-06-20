package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.remote.RemoteControlConnectionStatus
import io.github.vupoint.cokit.client.remote.RemoteControlDisableParams
import io.github.vupoint.cokit.client.remote.RemoteControlEnableParams
import io.github.vupoint.cokit.client.remote.RemoteControlEnvironmentId
import io.github.vupoint.cokit.client.remote.RemoteControlInstallationId
import io.github.vupoint.cokit.client.remote.RemoteControlClientId
import io.github.vupoint.cokit.client.remote.RemoteControlClientsListOrder
import io.github.vupoint.cokit.client.remote.RemoteControlClientsListParams
import io.github.vupoint.cokit.client.remote.RemoteControlClientsRevokeParams
import io.github.vupoint.cokit.client.remote.RemoteControlClientsRevokeResult
import io.github.vupoint.cokit.client.remote.RemoteControlManualPairingCode
import io.github.vupoint.cokit.client.remote.RemoteControlPairingCode
import io.github.vupoint.cokit.client.remote.RemoteControlPairingStartParams
import io.github.vupoint.cokit.client.remote.RemoteControlPairingStatusParams
import io.github.vupoint.cokit.client.remote.RemoteControlStatusReadParams
import io.github.vupoint.cokit.protocol.JsonRpcNotification
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.testing.FakeJsonRpcTransport
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

@OptIn(ExperimentalCodexApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RemoteControlRpcTest {
    @Test
    fun experimentalRemoteControlDescriptorsUseReadEnableDisableWireShapes() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val enableResult = async {
            fixture.client.request(
                CodexRpc.RemoteControl.Enable,
                RemoteControlEnableParams(ephemeral = true),
            )
        }
        runCurrent()

        val enable = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("remoteControl/enable", enable.method)
        assertEquals(true, enable.params!!.jsonObject["ephemeral"]?.jsonPrimitive?.booleanOrNull)
        fixture.transport.receive(JsonRpcResponse(enable.id, result = statusSnapshot(status = "connected")))

        val enabled = enableResult.await()
        assertEquals(RemoteControlConnectionStatus.Connected, enabled.status)
        assertEquals(RemoteControlInstallationId("install_123"), enabled.installationId)
        assertEquals("desktop-host", enabled.serverName)
        assertEquals(RemoteControlEnvironmentId("env_123"), enabled.environmentId)

        val statusResult = async {
            fixture.client.request(CodexRpc.RemoteControl.ReadStatus, RemoteControlStatusReadParams)
        }
        runCurrent()

        val status = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("remoteControl/status/read", status.method)
        assertNull(status.params)
        fixture.transport.receive(JsonRpcResponse(status.id, result = statusSnapshot(status = "connecting")))
        assertEquals(RemoteControlConnectionStatus.Connecting, statusResult.await().status)

        val disableResult = async {
            fixture.client.request(
                CodexRpc.RemoteControl.Disable,
                RemoteControlDisableParams(ephemeral = false),
            )
        }
        runCurrent()

        val disable = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("remoteControl/disable", disable.method)
        assertEquals(false, disable.params!!.jsonObject["ephemeral"]?.jsonPrimitive?.booleanOrNull)
        fixture.transport.receive(
            JsonRpcResponse(
                disable.id,
                result = statusSnapshot(status = "disabled", environmentId = null),
            ),
        )
        val disabled = disableResult.await()
        assertEquals(RemoteControlConnectionStatus.Disabled, disabled.status)
        assertNull(disabled.environmentId)
    }

    @Test
    fun decodesRemoteControlStatusChangedNotifications() {
        val notification = JsonRpcNotification(
            method = "remoteControl/status/changed",
            params = statusSnapshot(status = "errored"),
        ).toCodexNotification()

        val changed = assertIs<CodexNotification.RemoteControlStatusChanged>(notification)
        assertEquals(RemoteControlConnectionStatus.Errored, changed.status.status)
        assertEquals(RemoteControlInstallationId("install_123"), changed.status.installationId)
        assertEquals("desktop-host", changed.status.serverName)
        assertEquals(RemoteControlEnvironmentId("env_123"), changed.status.environmentId)
    }

    @Test
    fun experimentalRemotePairingDescriptorsUseStartAndStatusWireShapes() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val startResult = async {
            fixture.client.request(
                CodexRpc.RemoteControl.StartPairing,
                RemoteControlPairingStartParams(manualCode = true),
            )
        }
        runCurrent()

        val start = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("remoteControl/pairing/start", start.method)
        assertEquals(true, start.params!!.jsonObject["manualCode"]?.jsonPrimitive?.booleanOrNull)
        fixture.transport.receive(
            JsonRpcResponse(
                start.id,
                result = pairingStartResult(),
            ),
        )

        val pairing = startResult.await()
        assertEquals(RemoteControlPairingCode("pairing-code"), pairing.pairingCode)
        assertEquals(RemoteControlManualPairingCode("ABCD-EFGH"), pairing.manualPairingCode)
        assertEquals(RemoteControlEnvironmentId("environment-id"), pairing.environmentId)
        assertEquals(33_336_362_096L, pairing.expiresAt)
        assertFalse(pairing.toString().contains("pairing-code"))
        assertFalse(pairing.toString().contains("ABCD-EFGH"))

        val statusParams = RemoteControlPairingStatusParams(
            pairingCode = RemoteControlPairingCode("pairing-code"),
        )
        val statusResult = async {
            fixture.client.request(CodexRpc.RemoteControl.ReadPairingStatus, statusParams)
        }
        runCurrent()

        val status = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("remoteControl/pairing/status", status.method)
        assertEquals("pairing-code", status.params!!.jsonObject["pairingCode"]?.jsonPrimitive?.contentOrNull)
        assertNull(status.params!!.jsonObject["manualPairingCode"]?.jsonPrimitive?.contentOrNull)
        assertFalse(statusParams.toString().contains("pairing-code"))
        fixture.transport.receive(
            JsonRpcResponse(
                status.id,
                result = buildJsonObject { put("claimed", true) },
            ),
        )
        assertEquals(true, statusResult.await().claimed)
    }

    @Test
    fun experimentalRemoteClientDescriptorsUseListAndRevokeWireShapes() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val listResult = async {
            fixture.client.request(
                CodexRpc.RemoteControl.ListClients,
                RemoteControlClientsListParams(
                    environmentId = RemoteControlEnvironmentId("environment-id"),
                    cursor = CodexCursor("cursor-id"),
                    limit = 10,
                    order = RemoteControlClientsListOrder.Desc,
                ),
            )
        }
        runCurrent()

        val list = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("remoteControl/client/list", list.method)
        assertEquals("environment-id", list.params!!.jsonObject["environmentId"]?.jsonPrimitive?.contentOrNull)
        assertEquals("cursor-id", list.params!!.jsonObject["cursor"]?.jsonPrimitive?.contentOrNull)
        assertEquals("10", list.params!!.jsonObject["limit"]?.jsonPrimitive?.contentOrNull)
        assertEquals("desc", list.params!!.jsonObject["order"]?.jsonPrimitive?.contentOrNull)
        fixture.transport.receive(
            JsonRpcResponse(
                list.id,
                result = clientListResult(),
            ),
        )

        val clients = listResult.await()
        val client = clients.data.single()
        assertEquals(RemoteControlClientId("client-id"), client.clientId)
        assertEquals("Anton Phone", client.displayName)
        assertEquals("phone", client.deviceType)
        assertEquals("ios", client.platform)
        assertEquals("19.0", client.osVersion)
        assertEquals("iPhone", client.deviceModel)
        assertEquals("1.2.3", client.appVersion)
        assertEquals(1_772_694_000L, client.lastSeenAt)
        assertEquals(CodexCursor("next-cursor"), clients.nextCursor)
        assertFalse(client.toString().contains("client-id"))

        val revokeParams = RemoteControlClientsRevokeParams(
            environmentId = RemoteControlEnvironmentId("environment-id"),
            clientId = RemoteControlClientId("client-id"),
        )
        val revokeResult = async {
            fixture.client.request(CodexRpc.RemoteControl.RevokeClient, revokeParams)
        }
        runCurrent()

        val revoke = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("remoteControl/client/revoke", revoke.method)
        assertEquals("environment-id", revoke.params!!.jsonObject["environmentId"]?.jsonPrimitive?.contentOrNull)
        assertEquals("client-id", revoke.params!!.jsonObject["clientId"]?.jsonPrimitive?.contentOrNull)
        assertFalse(revokeParams.toString().contains("client-id"))
        fixture.transport.receive(JsonRpcResponse(revoke.id, result = JsonObject(emptyMap())))
        assertEquals(RemoteControlClientsRevokeResult, revokeResult.await())
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

    private fun statusSnapshot(
        status: String,
        environmentId: String? = "env_123",
    ): JsonObject =
        buildJsonObject {
            put("status", status)
            put("installationId", "install_123")
            put("serverName", "desktop-host")
            put("environmentId", environmentId)
        }

    private fun pairingStartResult(): JsonObject =
        buildJsonObject {
            put("pairingCode", "pairing-code")
            put("manualPairingCode", "ABCD-EFGH")
            put("environmentId", "environment-id")
            put("expiresAt", 33_336_362_096L)
        }

    private fun clientListResult(): JsonObject =
        buildJsonObject {
            put(
                "data",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("clientId", "client-id")
                            put("displayName", "Anton Phone")
                            put("deviceType", "phone")
                            put("platform", "ios")
                            put("osVersion", "19.0")
                            put("deviceModel", "iPhone")
                            put("appVersion", "1.2.3")
                            put("lastSeenAt", 1_772_694_000L)
                        },
                    )
                },
            )
            put("nextCursor", "next-cursor")
        }

    private data class ConnectedRpcClientFixture(
        val client: CodexRpcClient,
        val transport: FakeJsonRpcTransport,
    )
}
