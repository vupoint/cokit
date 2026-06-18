package io.github.cokit.client

import io.github.cokit.client.remote.RemoteControlConnectionStatus
import io.github.cokit.client.remote.RemoteControlDisableParams
import io.github.cokit.client.remote.RemoteControlEnableParams
import io.github.cokit.client.remote.RemoteControlEnvironmentId
import io.github.cokit.client.remote.RemoteControlInstallationId
import io.github.cokit.client.remote.RemoteControlStatusReadParams
import io.github.cokit.protocol.JsonRpcNotification
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import io.github.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
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

    private data class ConnectedRpcClientFixture(
        val client: CodexRpcClient,
        val transport: FakeJsonRpcTransport,
    )
}
