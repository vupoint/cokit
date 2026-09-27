package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.approvals.CommandApprovalRequest
import io.github.vupoint.cokit.client.mcp.McpElicitationRequest
import io.github.vupoint.cokit.protocol.CodexProtocolJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

class Release157CompatibilityTest {
    @Test
    fun commandApprovalPreservesStdinAndUnknownKinds() {
        for (kind in listOf("writeStdin", "futureAction")) {
            roundTrip(
                """{"threadId":"thr_1","turnId":"turn_1","itemId":"item_1","startedAtMs":1,"kind":"$kind"}""",
                CommandApprovalRequest.serializer(),
            )
        }
    }

    @Test
    fun initializePreservesExtensionDeclarations() {
        roundTrip(
            """{"extensions":{"openai/form":{}},"explicitGatewayOauth":true}""",
            InitializeCapabilities.serializer(),
        )
    }

    @Test
    fun openAiFormAliasesDecodeTheSameTypedRequest() {
        for (mode in listOf("openai/form", "openaiForm")) {
            val request = CodexProtocolJson.decodeFromJsonElement(
                McpElicitationRequest.serializer(),
                CodexProtocolJson.parseToJsonElement(
                    """{"serverName":"example","threadId":"thr_1","message":"Confirm","mode":"$mode","requestedSchema":{"type":"object"}}""",
                ),
            )
            assertIs<McpElicitationRequest.OpenAiForm>(request)
        }
    }

    @Test
    fun threadListRetainsSectionFilterOmissionNullAndValue() {
        for (fixture in listOf("{}", "{\"sectionId\":null}", "{\"sectionId\":\"section_1\",\"originators\":[\"desktop\"]}")) {
            roundTrip(fixture, ThreadListParams.serializer())
        }
    }

    @Test
    fun resumeAndForkRetainStableHistoryExclusion() {
        val fixture = """{"threadId":"thr_1","excludeTurns":true}"""
        roundTrip(fixture, ThreadResumeParams.serializer())
        roundTrip(fixture, ThreadForkParams.serializer())
    }

    @Test
    fun threadRetainsMetadataAndHistory() {
        roundTrip(
            """{"id":"thr_1","name":"Review","status":{"type":"active","activeFlags":["waitingOnApproval"]},"historyMode":"paginated","model":"test-model","reasoningEffort":"high","sessionId":"session_1","forkedFromId":"thr_0","ephemeral":false,"section":{"id":"section_1","name":"Work"},"turns":[{"id":"turn_1","status":"completed","itemsView":"notLoaded"}]}""",
            Thread.serializer(),
        )
    }

    @Test
    fun threadStatusNotificationPreservesStructuredStatus() {
        val notification = io.github.vupoint.cokit.protocol.JsonRpcNotification(
            method = "thread/status/changed",
            params = CodexProtocolJson.parseToJsonElement(
                """{"threadId":"thr_1","status":{"type":"active","activeFlags":["waitingOnApproval"]}}""",
            ),
        ).toCodexNotification()
        assertIs<CodexNotification.ThreadStatusChanged>(notification)
    }

    @Test
    fun sectionAppearancePreservesOmissionClearAndReplacement() {
        for (appearance in listOf("", ",\"appearance\":null", ",\"appearance\":{\"color\":\"blue\"}")) {
            roundTrip("{\"sectionId\":\"section_1\",\"name\":\"Work\"$appearance}", ThreadSectionUpdateParams.serializer())
        }
        roundTrip("""{"threadId":"thr_1","sectionId":null}""", ThreadSectionMoveParams.serializer())
        kotlin.test.assertFailsWith<kotlinx.serialization.SerializationException> {
            CodexProtocolJson.decodeFromJsonElement(ThreadListParams.serializer(), CodexProtocolJson.parseToJsonElement("""{"sectionId":{}}"""))
        }
    }

    @Test
    fun historyDescriptorsRetainItemsAndBothCursors() {
        assertEquals("thread/items/list", CodexRpc.Thread.ListItems.method)
        assertEquals("thread/revert", CodexRpc.Thread.Revert.method)
        assertEquals("thread/section/move", CodexRpc.Thread.MoveToSection.method)
        assertEquals("threadSection/list", CodexRpc.ThreadSection.List.method)
        assertEquals("threadSection/create", CodexRpc.ThreadSection.Create.method)
        assertEquals("threadSection/update", CodexRpc.ThreadSection.Update.method)
        assertEquals("threadSection/delete", CodexRpc.ThreadSection.Delete.method)
        roundTrip(
            """{"data":[{"turnId":"turn_1","item":{"type":"futureItem","id":"item_1","value":42},"startedAtMs":10,"completedAtMs":20}],"nextCursor":"next","backwardsCursor":"back"}""",
            ThreadItemsListResult.serializer(),
        )
        roundTrip(
            """{"thread":{"id":"thr_1"},"itemsBackwardsCursor":"items","turnsBackwardsCursor":"turns"}""",
            ThreadRevertResult.serializer(),
        )
    }

    @Test
    fun turnOptionsAndTimingRetainReleaseFields() {
        roundTrip(
            """{"threadId":"thr_1","input":[],"serviceTierForTurn":"default","turnTrigger":"scheduled","disabledPluginIds":[],"toolOutput":{"name":"lookup","output":"done"}}""",
            TurnStartParams.serializer(),
        )
        roundTrip(
            """{"id":"turn_1","status":"completed","startedAt":10,"completedAt":11,"durationMs":1000}""",
            Turn.serializer(),
        )
    }

    @Test
    fun permissionAndMcpSnapshotsRetainAvailabilityAndExplicitAccountTarget() {
        roundTrip("""{"id":"read-only","allowed":false}""", io.github.vupoint.cokit.client.environment.PermissionProfileSummary.serializer())
        roundTrip(
            """{"name":"example","authStatus":"notLoggedIn","runtimeStatus":"authenticationRequired","toolsError":"Sign in required","serverCapabilities":{"extensions":{"example":{}}},"httpOrigin":"https://example.invalid","pluginId":"example@market"}""",
            io.github.vupoint.cokit.client.mcp.McpServerStatus.serializer(),
        )
        roundTrip(
            """{"server":"example","uri":"example://resource","connectorId":"app_1","originCallId":"call_1","target":{"connectorId":"app_1","linkId":null}}""",
            io.github.vupoint.cokit.client.mcp.McpResourceReadParams.serializer(),
        )
    }

    @Test
    fun modelCatalogAndManagedRequirementsRetainReleaseMetadata() {
        roundTrip(
            """{"id":"model_1","model":"test-model","displayName":"Test","description":"Test model","hidden":false,"isDefault":true,"defaultReasoningEffort":"high","supportedReasoningEfforts":[],"modelSpecialty":"coding","multiAgentVersion":"v2","availableAccessPrograms":{"cyber":["future-program"]}}""",
            io.github.vupoint.cokit.client.models.ModelCatalogEntry.serializer(),
        )
        roundTrip(
            """{"allowedLoginMethods":[],"modelProvider":"openai","modelProviders":{"openai":{"wire_api":"responses"}},"allowBrowserAndComputerUse":false,"autoReview":{"requiredOnModels":["test-model"]},"inAppBrowser":{"allowExternalBrowserSettingsImport":false},"cliAuthCredentialsStore":"keyring","chatgptBaseUrl":"https://example.invalid","additionalDeveloperInstructions":"Managed instructions"}""",
            io.github.vupoint.cokit.client.policy.ManagedPolicyRequirements.serializer(),
        )
    }

    @Test
    fun toolOutputContentSupportsMultimodalPayloadsAndRejectsInvalidImages() {
        roundTrip(
            """{"name":"lookup","output":[{"type":"input_text","text":"done"},{"type":"input_image","image_url":"https://example.invalid/image.png","detail":"high"},{"type":"input_image","file_id":"file_1"},{"type":"input_audio","audio_url":"https://example.invalid/audio.wav"},{"type":"encrypted_content","encrypted_content":"opaque"}]}""",
            TurnToolOutput.serializer(),
        )
        kotlin.test.assertFailsWith<IllegalArgumentException> { ToolOutputContent.Image() }
        kotlin.test.assertFailsWith<IllegalArgumentException> { ToolOutputContent.Image(imageUrl = "url", fileId = "id") }
        kotlin.test.assertFailsWith<kotlinx.serialization.SerializationException> {
            CodexProtocolJson.decodeFromJsonElement(ToolOutputBody.serializer(), CodexProtocolJson.parseToJsonElement("42"))
        }
    }

    @Test
    fun threadResumeRetainsEffectiveConfigurationAndHydrationCursors() {
        roundTrip(
            """{"thread":{"id":"thr_1"},"model":"test-model","modelProvider":"openai","cwd":"/path/to/project","approvalPolicy":"on-request","approvalsReviewer":"user","sandbox":{"type":"readOnly"},"reasoningEffort":"high","serviceTier":"default","instructionSources":["/path/to/project/AGENTS.md"],"disabledPluginIds":["example@market"],"itemsBackwardsCursor":"items","turnsBackwardsCursor":"turns","collaborationMode":{"mode":"default","settings":{"model":"test-model"}}}""",
            ThreadResumeResult.serializer(),
        )
    }

    private fun <T> roundTrip(fixture: String, serializer: KSerializer<T>) {
        val expected = CodexProtocolJson.parseToJsonElement(fixture)
        val decoded = CodexProtocolJson.decodeFromJsonElement(serializer, expected)
        assertEquals(expected, CodexProtocolJson.encodeToJsonElement(serializer, decoded))
    }
}
