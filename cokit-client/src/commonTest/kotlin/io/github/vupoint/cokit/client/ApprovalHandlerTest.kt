package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.approvals.ApprovalDecision
import io.github.vupoint.cokit.client.approvals.CommandApprovalRequest
import io.github.vupoint.cokit.client.approvals.FileChangeApprovalRequest
import io.github.vupoint.cokit.client.approvals.FileChangeKind
import io.github.vupoint.cokit.client.approvals.FileChangeSummary
import io.github.vupoint.cokit.client.approvals.FileSystemPermissionAccess
import io.github.vupoint.cokit.client.approvals.FileSystemPermissionEntry
import io.github.vupoint.cokit.client.approvals.FileSystemPermissionPath
import io.github.vupoint.cokit.client.approvals.PermissionApprovalRequest
import io.github.vupoint.cokit.client.approvals.PermissionApprovalResponse
import io.github.vupoint.cokit.client.approvals.PermissionEnvironmentId
import io.github.vupoint.cokit.client.approvals.PermissionFileSystem
import io.github.vupoint.cokit.client.approvals.PermissionGrantScope
import io.github.vupoint.cokit.client.approvals.PermissionNetwork
import io.github.vupoint.cokit.client.approvals.PermissionProfile
import io.github.vupoint.cokit.protocol.JsonRpcId
import io.github.vupoint.cokit.protocol.JsonRpcRequest
import io.github.vupoint.cokit.protocol.JsonRpcResponse
import io.github.vupoint.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ApprovalHandlerTest {
    @Test
    fun commandApprovalRequestUsesSdkValueTypes() {
        val request = CommandApprovalRequest(
            threadId = ThreadId("thr_123"),
            turnId = TurnId("turn_123"),
            itemId = "item_123",
            command = "git status",
            cwd = CodexHostPath("/path/to/project"),
        )

        assertEquals(ThreadId("thr_123"), request.threadId)
        assertEquals(TurnId("turn_123"), request.turnId)
        assertEquals(CodexHostPath("/path/to/project"), request.cwd)
    }

    @Test
    fun fileChangeApprovalRequestUsesSdkValueTypes() {
        val request = FileChangeApprovalRequest(
            threadId = ThreadId("thr_123"),
            turnId = TurnId("turn_123"),
            itemId = ItemId("item_123"),
            startedAtMs = 1_776_000_000_000,
            reason = "Review proposed patch",
            grantRoot = CodexHostPath("/path/to/project"),
        )
        val summary = FileChangeSummary(
            path = CodexHostPath("/path/to/project/src/Main.kt"),
            kind = FileChangeKind("modify"),
            diff = "@@ -1 +1 @@",
        )

        assertEquals(ThreadId("thr_123"), request.threadId)
        assertEquals(TurnId("turn_123"), request.turnId)
        assertEquals(ItemId("item_123"), request.itemId)
        assertEquals(CodexHostPath("/path/to/project"), request.grantRoot)
        assertEquals(1_776_000_000_000, request.startedAtMs)
        assertEquals(CodexHostPath("/path/to/project/src/Main.kt"), summary.path)
    }

    @Test
    fun permissionApprovalRequestUsesSdkValueTypes() {
        val profile = PermissionProfile(
            fileSystem = PermissionFileSystem(
                write = listOf(CodexHostPath("/path/to/project")),
                entries = listOf(
                    FileSystemPermissionEntry(
                        access = FileSystemPermissionAccess.Read,
                        path = FileSystemPermissionPath.ProjectRoots(subpath = "src"),
                    ),
                ),
            ),
            network = PermissionNetwork(enabled = true),
        )
        val request = PermissionApprovalRequest(
            threadId = ThreadId("thr_123"),
            turnId = TurnId("turn_123"),
            itemId = ItemId("item_123"),
            startedAtMs = 1_776_000_000_000,
            environmentId = PermissionEnvironmentId("local"),
            cwd = CodexHostPath("/path/to/project"),
            reason = "Select a workspace root",
            permissions = profile,
        )

        assertEquals(ThreadId("thr_123"), request.threadId)
        assertEquals(TurnId("turn_123"), request.turnId)
        assertEquals(ItemId("item_123"), request.itemId)
        assertEquals(PermissionEnvironmentId("local"), request.environmentId)
        assertEquals(CodexHostPath("/path/to/project"), request.cwd)
        assertEquals(CodexHostPath("/path/to/project"), request.permissions.fileSystem?.write?.single())
        assertEquals(true, request.permissions.network?.enabled)
    }

    @Test
    fun commandApprovalDeclinesWhenNoHandlerIsRegistered() = runTest {
        val fixture = connectedClientFixture(backgroundScope)

        fixture.transport.receive(
            JsonRpcRequest(
                id = JsonRpcId.Number(99),
                method = "item/commandExecution/requestApproval",
                params = buildJsonObject {
                    put("threadId", "thr_123")
                    put("turnId", "turn_123")
                    put("itemId", "item_123")
                    put("command", "git status")
                    put("cwd", "/tmp/project")
                },
            ),
        )
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals("decline", response.result?.jsonObject?.get("decision")?.jsonPrimitive?.content)
    }

    @Test
    fun fileChangeApprovalServerRequestsAreTypedAndDeclineByDefault() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        val serverRequest = async { fixture.client.serverRequests.first() }

        fixture.transport.receive(fileChangeApprovalRequest(id = 99))
        runCurrent()

        val fileChangeApproval = assertIs<CodexServerRequest.FileChangeApproval>(serverRequest.await())
        assertEquals(ItemId("item_123"), fileChangeApproval.request.itemId)
        assertEquals("Review proposed patch", fileChangeApproval.request.reason)
        assertEquals(CodexHostPath("/path/to/project"), fileChangeApproval.request.grantRoot)

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals("decline", response.result?.jsonObject?.get("decision")?.jsonPrimitive?.content)
    }

    @Test
    fun fileChangeApprovalHandlerCanReturnTypedDecision() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        fixture.client.registerFileChangeApprovalHandler { request ->
            assertEquals(ItemId("item_123"), request.itemId)
            assertEquals(CodexHostPath("/path/to/project"), request.grantRoot)
            ApprovalDecision.AcceptForSession
        }

        fixture.transport.receive(fileChangeApprovalRequest(id = 99))
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals("acceptForSession", response.result?.jsonObject?.get("decision")?.jsonPrimitive?.content)
    }

    @Test
    fun permissionApprovalServerRequestsAreTypedAndDeclineByDefault() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        val serverRequest = async { fixture.client.serverRequests.first() }

        fixture.transport.receive(permissionApprovalRequest(id = 99))
        runCurrent()

        val permissionApproval = assertIs<CodexServerRequest.PermissionApproval>(serverRequest.await())
        assertEquals(ItemId("item_123"), permissionApproval.request.itemId)
        assertEquals(PermissionEnvironmentId("local"), permissionApproval.request.environmentId)
        assertEquals(CodexHostPath("/path/to/project"), permissionApproval.request.cwd)
        assertEquals(CodexHostPath("/path/to/project"), permissionApproval.request.permissions.fileSystem?.write?.single())
        assertEquals(true, permissionApproval.request.permissions.network?.enabled)

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals(emptySet(), response.result?.jsonObject?.get("permissions")?.jsonObject?.keys)
        assertFalse(response.result?.jsonObject?.containsKey("decision") ?: false)
        assertFalse(response.result?.jsonObject?.containsKey("scope") ?: false)
    }

    @Test
    fun permissionApprovalHandlerCanReturnTurnScopedGrant() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        fixture.client.registerPermissionApprovalHandler { request ->
            assertEquals(ItemId("item_123"), request.itemId)
            assertEquals(CodexHostPath("/path/to/project"), request.cwd)
            PermissionApprovalResponse(
                permissions = PermissionProfile(
                    fileSystem = PermissionFileSystem(
                        write = listOf(CodexHostPath("/path/to/project")),
                    ),
                ),
                scope = PermissionGrantScope.Turn,
            )
        }

        fixture.transport.receive(permissionApprovalRequest(id = 99))
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        val result = response.result?.jsonObject
        val permissions = result?.get("permissions")?.jsonObject
        val fileSystem = permissions?.get("fileSystem")?.jsonObject
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals("/path/to/project", fileSystem?.get("write")?.jsonArray?.single()?.jsonPrimitive?.content)
        assertEquals("turn", result?.get("scope")?.jsonPrimitive?.content)
    }

    @Test
    fun permissionApprovalHandlerCanReturnSessionScopedGrantWithAutoReview() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        fixture.client.registerPermissionApprovalHandler {
            PermissionApprovalResponse(
                permissions = PermissionProfile(
                    network = PermissionNetwork(enabled = true),
                ),
                scope = PermissionGrantScope.Session,
                strictAutoReview = true,
            )
        }

        fixture.transport.receive(permissionApprovalRequest(id = 99))
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        val result = response.result?.jsonObject
        val permissions = result?.get("permissions")?.jsonObject
        val network = permissions?.get("network")?.jsonObject
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals(true, network?.get("enabled")?.jsonPrimitive?.content?.toBooleanStrict())
        assertEquals("session", result?.get("scope")?.jsonPrimitive?.content)
        assertEquals(true, result?.get("strictAutoReview")?.jsonPrimitive?.content?.toBooleanStrict())
    }

    @Test
    fun malformedPermissionApprovalParamsReturnInvalidParamsWithoutCallingTypedHandler() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        var handlerCalled = false
        fixture.client.registerPermissionApprovalHandler {
            handlerCalled = true
            PermissionApprovalResponse.Decline
        }

        fixture.transport.receive(
            JsonRpcRequest(
                id = JsonRpcId.Number(99),
                method = "item/permissions/requestApproval",
                params = buildJsonObject {
                    put("threadId", "thr_123")
                    put("turnId", "turn_123")
                    put("itemId", "item_123")
                },
            ),
        )
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals(-32602, response.error?.code)
        assertEquals(
            "Invalid params for item/permissions/requestApproval",
            response.error?.message,
        )
        assertFalse(handlerCalled)
    }

    @Test
    fun permissionApprovalHandlerExceptionsReturnGenericJsonRpcError() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        fixture.client.registerPermissionApprovalHandler {
            error("internal detail should not leak")
        }

        fixture.transport.receive(permissionApprovalRequest(id = 99))
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals(-32000, response.error?.code)
        assertEquals("Server request handler failed", response.error?.message)
        assertFalse(response.toString().contains("internal detail"))
    }

    @Test
    fun serverRequestHandlersUseSdkRequestAndResponseTypes() = runTest {
        val fixture = connectedClientFixture(backgroundScope)
        fixture.client.registerServerRequestHandler("custom/request") { request ->
            assertEquals("custom/request", request.method)
            assertEquals("""{"value":"kept"}""", request.params?.toJsonString())
            CodexServerResponse.Result(CodexJsonPayload.parse("""{"decision":"decline"}"""))
        }

        fixture.transport.receive(
            JsonRpcRequest(
                id = JsonRpcId.Number(99),
                method = "custom/request",
                params = buildJsonObject {
                    put("value", "kept")
                },
            ),
        )
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertTrue(response.result.toString().contains("decline"))
    }

    @Test
    fun malformedCommandApprovalParamsReturnInvalidParamsWithoutCallingTypedHandler() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        var handlerCalled = false
        fixture.client.registerCommandApprovalHandler {
            handlerCalled = true
            ApprovalDecision.Accept
        }

        fixture.transport.receive(
            JsonRpcRequest(
                id = JsonRpcId.Number(99),
                method = "item/commandExecution/requestApproval",
                params = buildJsonObject {
                    put("threadId", "thr_123")
                    put("turnId", "turn_123")
                    put("itemId", "item_123")
                },
            ),
        )
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals(-32602, response.error?.code)
        assertEquals(
            "Invalid params for item/commandExecution/requestApproval",
            response.error?.message,
        )
        assertFalse(handlerCalled)
    }

    @Test
    fun commandApprovalHandlerExceptionsReturnGenericJsonRpcError() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        fixture.client.registerCommandApprovalHandler {
            error("internal detail should not leak")
        }

        fixture.transport.receive(commandApprovalRequest(id = 99))
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals(-32000, response.error?.code)
        assertEquals("Server request handler failed", response.error?.message)
        assertFalse(response.toString().contains("internal detail"))
    }

    @Test
    fun commandApprovalHandlerEncodesTypedDecisionsExactly() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        val cases = listOf(
            ApprovalDecision.Accept to "accept",
            ApprovalDecision.AcceptForSession to "acceptForSession",
            ApprovalDecision.Decline to "decline",
            ApprovalDecision.Cancel to "cancel",
        )

        cases.forEachIndexed { index, (decision, expectedValue) ->
            fixture.client.registerCommandApprovalHandler { decision }
            fixture.transport.receive(commandApprovalRequest(id = index.toLong()))
            runCurrent()

            val response = fixture.transport.sent.last() as JsonRpcResponse
            assertEquals(JsonRpcId.Number(index.toLong()), response.id)
            assertEquals(expectedValue, response.result?.jsonObject?.get("decision")?.jsonPrimitive?.content)
        }
    }

    @Test
    fun eventsExposeTypedNotificationEnvelope() = runTest {
        val fixture = connectedClientFixture(backgroundScope)
        val event = async {
            fixture.client.events.first()
        }

        fixture.transport.receive(
            io.github.vupoint.cokit.protocol.JsonRpcNotification(
                method = "thread/started",
                params = buildJsonObject {
                    put("threadId", "thr_123")
                },
            ),
        )
        runCurrent()

        val notification = event.await() as CodexEvent.Notification
        assertEquals("thread/started", notification.method)
        assertEquals("""{"threadId":"thr_123"}""", notification.params?.toJsonString())
    }

    private suspend fun TestScope.connectedClientFixture(
        scope: CoroutineScope,
    ): ConnectedClientFixture {
        val transport = FakeJsonRpcTransport()
        val client = async {
            CodexAppServerClient.connect(
                CodexClientOptions(
                    transport = transport,
                    clientInfo = ClientInfo("cokit_test", "CoKit Test", "0.1.0"),
                    scope = scope,
                ),
            )
        }
        runCurrent()
        val initialize = transport.sent.single() as JsonRpcRequest
        transport.receive(JsonRpcResponse(initialize.id, result = JsonObject(emptyMap())))
        return ConnectedClientFixture(client.await(), transport)
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

    private fun commandApprovalRequest(id: Long): JsonRpcRequest = JsonRpcRequest(
        id = JsonRpcId.Number(id),
        method = "item/commandExecution/requestApproval",
        params = buildJsonObject {
            put("threadId", "thr_123")
            put("turnId", "turn_123")
            put("itemId", "item_123")
            put("command", "git status")
            put("cwd", "/path/to/project")
        },
    )

    private fun fileChangeApprovalRequest(id: Long): JsonRpcRequest = JsonRpcRequest(
        id = JsonRpcId.Number(id),
        method = "item/fileChange/requestApproval",
        params = buildJsonObject {
            put("threadId", "thr_123")
            put("turnId", "turn_123")
            put("itemId", "item_123")
            put("startedAtMs", 1_776_000_000_000)
            put("reason", "Review proposed patch")
            put("grantRoot", "/path/to/project")
        },
    )

    private fun permissionApprovalRequest(id: Long): JsonRpcRequest = JsonRpcRequest(
        id = JsonRpcId.Number(id),
        method = "item/permissions/requestApproval",
        params = buildJsonObject {
            put("threadId", "thr_123")
            put("turnId", "turn_123")
            put("itemId", "item_123")
            put("startedAtMs", 1_776_000_000_000)
            put("environmentId", "local")
            put("cwd", "/path/to/project")
            put("reason", "Select a workspace root")
            put(
                "permissions",
                buildJsonObject {
                    put(
                        "fileSystem",
                        buildJsonObject {
                            putJsonArray("write") {
                                add("/path/to/project")
                            }
                            putJsonArray("entries") {
                                add(
                                    buildJsonObject {
                                        put("access", "read")
                                        put(
                                            "path",
                                            buildJsonObject {
                                                put("type", "special")
                                                put(
                                                    "value",
                                                    buildJsonObject {
                                                        put("kind", "project_roots")
                                                        put("subpath", "src")
                                                    },
                                                )
                                            },
                                        )
                                    },
                                )
                            }
                        },
                    )
                    put(
                        "network",
                        buildJsonObject {
                            put("enabled", true)
                        },
                    )
                },
            )
        },
    )

    private data class ConnectedClientFixture(
        val client: CodexAppServerClient,
        val transport: FakeJsonRpcTransport,
    )

    private data class ConnectedRpcClientFixture(
        val client: CodexRpcClient,
        val transport: FakeJsonRpcTransport,
    )
}
