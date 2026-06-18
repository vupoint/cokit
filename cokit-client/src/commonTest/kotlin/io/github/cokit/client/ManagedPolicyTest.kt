package io.github.cokit.client

import io.github.cokit.client.environment.PermissionProfileId
import io.github.cokit.client.policy.ManagedNetworkPermission
import io.github.cokit.client.policy.ManagedPolicyReadParams
import io.github.cokit.client.policy.ManagedResidencyRequirement
import io.github.cokit.client.policy.ManagedWebSearchMode
import io.github.cokit.client.policy.ManagedWindowsSandboxSetupMode
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import io.github.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ManagedPolicyTest {
    @Test
    fun configRequirementsReadDescriptorDecodesManagedPolicyConstraints() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val result = async {
            fixture.client.request(CodexRpc.Config.ReadRequirements, ManagedPolicyReadParams)
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("configRequirements/read", request.method)
        assertNull(request.params)

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put("requirements", buildJsonObject {
                        put(
                            "allowedApprovalPolicies",
                            buildJsonArray {
                                add("on-request")
                                add("never")
                            },
                        )
                        put(
                            "allowedSandboxModes",
                            buildJsonArray {
                                add("read-only")
                                add("workspace-write")
                            },
                        )
                        put(
                            "allowedWebSearchModes",
                            buildJsonArray {
                                add("disabled")
                                add("live")
                            },
                        )
                        put(
                            "allowedWindowsSandboxImplementations",
                            buildJsonArray {
                                add("elevated")
                            },
                        )
                        put("allowedPermissionProfiles", buildJsonObject {
                            put("read-only", true)
                            put("full-access", false)
                        })
                        put("defaultPermissions", "read-only")
                        put("allowManagedHooksOnly", true)
                        put("allowAppshots", false)
                        put("allowRemoteControl", false)
                        put("computerUse", buildJsonObject {
                            put("allowLockedComputerUse", false)
                        })
                        put("featureRequirements", buildJsonObject {
                            put("newFeature", true)
                        })
                        put("enforceResidency", "us")
                        put("network", buildJsonObject {
                            put("enabled", true)
                            put("managedAllowedDomainsOnly", true)
                            put("domains", buildJsonObject {
                                put("example.com", "allow")
                                put("blocked.example", "deny")
                            })
                            put("unixSockets", buildJsonObject {
                                put("/path/to/socket", "allow")
                            })
                            put("allowLocalBinding", false)
                        })
                    })
                },
            ),
        )

        val requirements = result.await().requirements!!
        assertEquals(listOf(ApprovalPolicy.OnRequest, ApprovalPolicy.Never), requirements.allowedApprovalPolicies)
        assertEquals(listOf(SandboxPolicy.ReadOnly, SandboxPolicy.WorkspaceWrite), requirements.allowedSandboxModes)
        assertEquals(listOf(ManagedWebSearchMode.Disabled, ManagedWebSearchMode.Live), requirements.allowedWebSearchModes)
        assertEquals(
            listOf(ManagedWindowsSandboxSetupMode.Elevated),
            requirements.allowedWindowsSandboxImplementations,
        )
        assertEquals(
            mapOf(
                PermissionProfileId("read-only") to true,
                PermissionProfileId("full-access") to false,
            ),
            requirements.allowedPermissionProfiles,
        )
        assertEquals(PermissionProfileId("read-only"), requirements.defaultPermissions)
        assertEquals(true, requirements.allowManagedHooksOnly)
        assertEquals(false, requirements.allowAppshots)
        assertEquals(false, requirements.allowRemoteControl)
        assertEquals(false, requirements.computerUse?.allowLockedComputerUse)
        assertEquals(mapOf("newFeature" to true), requirements.featureRequirements)
        assertEquals(ManagedResidencyRequirement.Us, requirements.enforceResidency)
        assertEquals(true, requirements.network?.enabled)
        assertEquals(true, requirements.network?.managedAllowedDomainsOnly)
        assertEquals(
            mapOf(
                "example.com" to ManagedNetworkPermission.Allow,
                "blocked.example" to ManagedNetworkPermission.Deny,
            ),
            requirements.network?.domains,
        )
        assertEquals(
            mapOf("/path/to/socket" to ManagedNetworkPermission.Allow),
            requirements.network?.unixSockets,
        )
        assertEquals(false, requirements.network?.allowLocalBinding)
    }

    @Test
    fun configRequirementsReadDecodesNoRequirements() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val result = async {
            fixture.client.request(CodexRpc.Config.ReadRequirements, ManagedPolicyReadParams)
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put("requirements", null)
                },
            ),
        )

        assertNull(result.await().requirements)
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
