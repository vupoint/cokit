package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.auth.AccountReadParams
import io.github.vupoint.cokit.client.auth.AccountReadResult
import io.github.vupoint.cokit.client.auth.AccountRateLimitsReadParams
import io.github.vupoint.cokit.client.auth.AccountRateLimitsResult
import io.github.vupoint.cokit.client.auth.AccountUsageReadParams
import io.github.vupoint.cokit.client.auth.AccountUsageResult
import io.github.vupoint.cokit.client.auth.AccountWorkspaceMessagesReadParams
import io.github.vupoint.cokit.client.auth.AccountWorkspaceMessagesResult
import io.github.vupoint.cokit.client.auth.CancelLoginAccountParams
import io.github.vupoint.cokit.client.auth.CancelLoginAccountResult
import io.github.vupoint.cokit.client.auth.ConsumeAccountRateLimitResetCreditParams
import io.github.vupoint.cokit.client.auth.ConsumeAccountRateLimitResetCreditResult
import io.github.vupoint.cokit.client.auth.LoginAccountParams
import io.github.vupoint.cokit.client.auth.LoginAccountResult
import io.github.vupoint.cokit.client.auth.LogoutAccountParams
import io.github.vupoint.cokit.client.auth.SendAddCreditsNudgeEmailParams
import io.github.vupoint.cokit.client.auth.SendAddCreditsNudgeEmailResult
import io.github.vupoint.cokit.client.commands.CommandExecParams
import io.github.vupoint.cokit.client.commands.CommandExecResizeParams
import io.github.vupoint.cokit.client.commands.CommandExecResult
import io.github.vupoint.cokit.client.commands.CommandExecTerminateParams
import io.github.vupoint.cokit.client.commands.CommandExecWriteParams
import io.github.vupoint.cokit.client.config.ConfigBatchWriteParams
import io.github.vupoint.cokit.client.config.ConfigReadParams
import io.github.vupoint.cokit.client.config.ConfigReadResult
import io.github.vupoint.cokit.client.config.ConfigValueWriteParams
import io.github.vupoint.cokit.client.config.ConfigWriteResult
import io.github.vupoint.cokit.client.environment.CollaborationModeListParams
import io.github.vupoint.cokit.client.environment.CollaborationModeListResult
import io.github.vupoint.cokit.client.environment.EnvironmentAddParams
import io.github.vupoint.cokit.client.environment.PermissionProfileListParams
import io.github.vupoint.cokit.client.environment.PermissionProfileListResult
import io.github.vupoint.cokit.client.extensions.AppsListParams
import io.github.vupoint.cokit.client.extensions.AppsListResult
import io.github.vupoint.cokit.client.extensions.AppsInstalledParams
import io.github.vupoint.cokit.client.extensions.AppsInstalledResult
import io.github.vupoint.cokit.client.extensions.HooksListParams
import io.github.vupoint.cokit.client.extensions.HooksListResult
import io.github.vupoint.cokit.client.filesystem.FilesystemGetMetadataParams
import io.github.vupoint.cokit.client.filesystem.FilesystemGetMetadataResult
import io.github.vupoint.cokit.client.filesystem.FilesystemCopyParams
import io.github.vupoint.cokit.client.filesystem.FilesystemCreateDirectoryParams
import io.github.vupoint.cokit.client.filesystem.FilesystemReadDirectoryParams
import io.github.vupoint.cokit.client.filesystem.FilesystemReadDirectoryResult
import io.github.vupoint.cokit.client.filesystem.FilesystemReadFileParams
import io.github.vupoint.cokit.client.filesystem.FilesystemReadFileResult
import io.github.vupoint.cokit.client.filesystem.FilesystemRemoveParams
import io.github.vupoint.cokit.client.filesystem.FilesystemUnwatchParams
import io.github.vupoint.cokit.client.filesystem.FilesystemWriteFileParams
import io.github.vupoint.cokit.client.filesystem.FilesystemWatchParams
import io.github.vupoint.cokit.client.filesystem.FilesystemWatchResult
import io.github.vupoint.cokit.client.models.ModelListParams
import io.github.vupoint.cokit.client.models.ModelListResult
import io.github.vupoint.cokit.client.models.ModelProviderCapabilities
import io.github.vupoint.cokit.client.models.ModelProviderCapabilitiesReadParams
import io.github.vupoint.cokit.client.mcp.McpConfigReloadParams
import io.github.vupoint.cokit.client.mcp.McpResourceReadParams
import io.github.vupoint.cokit.client.mcp.McpResourceReadResult
import io.github.vupoint.cokit.client.mcp.McpServerOauthLoginParams
import io.github.vupoint.cokit.client.mcp.McpServerOauthLoginResult
import io.github.vupoint.cokit.client.mcp.McpServerStatusListParams
import io.github.vupoint.cokit.client.mcp.McpServerStatusListResult
import io.github.vupoint.cokit.client.mcp.McpServerToolCallParams
import io.github.vupoint.cokit.client.mcp.McpServerToolCallResult
import io.github.vupoint.cokit.client.plugins.MarketplaceAddParams
import io.github.vupoint.cokit.client.plugins.MarketplaceAddResult
import io.github.vupoint.cokit.client.plugins.MarketplaceRemoveParams
import io.github.vupoint.cokit.client.plugins.MarketplaceRemoveResult
import io.github.vupoint.cokit.client.plugins.MarketplaceUpgradeParams
import io.github.vupoint.cokit.client.plugins.MarketplaceUpgradeResult
import io.github.vupoint.cokit.client.plugins.PluginInstallParams
import io.github.vupoint.cokit.client.plugins.PluginInstallResult
import io.github.vupoint.cokit.client.plugins.PluginInstalledParams
import io.github.vupoint.cokit.client.plugins.PluginInstalledResult
import io.github.vupoint.cokit.client.plugins.PluginListParams
import io.github.vupoint.cokit.client.plugins.PluginListResult
import io.github.vupoint.cokit.client.plugins.PluginReadParams
import io.github.vupoint.cokit.client.plugins.PluginReadResult
import io.github.vupoint.cokit.client.plugins.PluginSkillReadParams
import io.github.vupoint.cokit.client.plugins.PluginSkillReadResult
import io.github.vupoint.cokit.client.plugins.PluginUninstallParams
import io.github.vupoint.cokit.client.policy.ManagedPolicyReadParams
import io.github.vupoint.cokit.client.policy.ManagedPolicyReadResult
import io.github.vupoint.cokit.client.process.ProcessKillParams
import io.github.vupoint.cokit.client.process.ProcessResizePtyParams
import io.github.vupoint.cokit.client.process.ProcessSpawnParams
import io.github.vupoint.cokit.client.process.ProcessWriteStdinParams
import io.github.vupoint.cokit.client.remote.RemoteControlDisableParams
import io.github.vupoint.cokit.client.remote.RemoteControlEnableParams
import io.github.vupoint.cokit.client.remote.RemoteControlClientsListParams
import io.github.vupoint.cokit.client.remote.RemoteControlClientsListResult
import io.github.vupoint.cokit.client.remote.RemoteControlClientsRevokeParams
import io.github.vupoint.cokit.client.remote.RemoteControlClientsRevokeResult
import io.github.vupoint.cokit.client.remote.RemoteControlPairingStartParams
import io.github.vupoint.cokit.client.remote.RemoteControlPairingStartResult
import io.github.vupoint.cokit.client.remote.RemoteControlPairingStatusParams
import io.github.vupoint.cokit.client.remote.RemoteControlPairingStatusResult
import io.github.vupoint.cokit.client.remote.RemoteControlStatusReadParams
import io.github.vupoint.cokit.client.remote.RemoteControlStatusSnapshot
import io.github.vupoint.cokit.client.review.ReviewStartParams
import io.github.vupoint.cokit.client.review.ReviewStartResult
import io.github.vupoint.cokit.client.skills.SkillConfigWriteParams
import io.github.vupoint.cokit.client.skills.SkillConfigWriteResult
import io.github.vupoint.cokit.client.skills.SkillsExtraRootsSetParams
import io.github.vupoint.cokit.client.skills.SkillsListParams
import io.github.vupoint.cokit.client.skills.SkillsListResult
import io.github.vupoint.cokit.client.auth.GatewayOAuthReadResult
import kotlinx.serialization.KSerializer

/**
 * Typed binding between an app-server method name, its request parameters, and its result.
 *
 * Obtain descriptors from [CodexRpc] and pass them to [CodexClient.request].
 *
 * @property method Exact JSON-RPC method name sent to the app-server.
 * @property paramsSerializer Parameter encoder; null omits the wire `params` field.
 * @property resultSerializer Decoder for a present result payload.
 * @property emptyResult Fallback when the decoded result is null; null means a payload is required.
 * @property requiresExperimentalApi Whether the client rejects this descriptor unless experimental
 * initialization capabilities were enabled.
 */
class CodexRpcMethod<P : Any, R : Any> internal constructor(
    val method: String,
    val paramsSerializer: KSerializer<P>?,
    val resultSerializer: KSerializer<R>,
    val emptyResult: R? = null,
    val requiresExperimentalApi: Boolean = false,
)

/**
 * Catalog of typed app-server request descriptors, grouped by protocol domain.
 *
 * Call [CodexClient.request] with a descriptor and its matching parameter model.
 * Read/list operations return requested data; [CodexRpc.Command.Exec] returns command completion.
 * For asynchronous operations such as [CodexRpc.Turn.Start], subsequent execution is streamed
 * through [CodexClient.notifications].
 * Surfaces marked [ExperimentalCodexApi] require Kotlin opt-in. Descriptors with
 * [CodexRpcMethod.requiresExperimentalApi] also require experimental initialization.
 */
object CodexRpc {
    /** Experimental project catalog and organization operations. */
    @ExperimentalCodexApi
    object Project {
        /** Lists a page of projects using the requested sort order. */
        val List: CodexRpcMethod<ProjectListParams, ProjectListResult> = CodexRpcMethod(
            method = "project/list",
            paramsSerializer = ProjectListParams.serializer(),
            resultSerializer = ProjectListResult.serializer(),
            requiresExperimentalApi = true,
        )

        /** Reads one project. */
        val Read: CodexRpcMethod<ProjectReadParams, ProjectResult> = CodexRpcMethod(
            method = "project/read",
            paramsSerializer = ProjectReadParams.serializer(),
            resultSerializer = ProjectResult.serializer(),
            requiresExperimentalApi = true,
        )

        /** Creates a project. */
        val Create: CodexRpcMethod<ProjectCreateParams, ProjectResult> = CodexRpcMethod(
            method = "project/create",
            paramsSerializer = ProjectCreateParams.serializer(),
            resultSerializer = ProjectResult.serializer(),
            requiresExperimentalApi = true,
        )

        /** Imports a project with optional existing thread associations. */
        val Import: CodexRpcMethod<ProjectImportParams, ProjectResult> = CodexRpcMethod(
            method = "project/import",
            paramsSerializer = ProjectImportParams.serializer(),
            resultSerializer = ProjectResult.serializer(),
            requiresExperimentalApi = true,
        )

        /** Updates the supplied project fields. */
        val Update: CodexRpcMethod<ProjectUpdateParams, ProjectResult> = CodexRpcMethod(
            method = "project/update",
            paramsSerializer = ProjectUpdateParams.serializer(),
            resultSerializer = ProjectResult.serializer(),
            requiresExperimentalApi = true,
        )

        /** Moves a project in the server organization. */
        val Move: CodexRpcMethod<ProjectMoveParams, CodexRpcUnit> = CodexRpcMethod(
            method = "project/move",
            paramsSerializer = ProjectMoveParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            requiresExperimentalApi = true,
            emptyResult = CodexRpcUnit,
        )

        /** Deletes the identified project. */
        val Delete: CodexRpcMethod<ProjectDeleteParams, CodexRpcUnit> = CodexRpcMethod(
            method = "project/delete",
            paramsSerializer = ProjectDeleteParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            requiresExperimentalApi = true,
            emptyResult = CodexRpcUnit,
        )

    }

    /** Experimental queued input operations for a thread. */
    @ExperimentalCodexApi
    object ThreadQueue {
        /** Queues input for a thread. */
        val Add: CodexRpcMethod<ThreadQueueAddParams, ThreadQueueSubmissionResult> = CodexRpcMethod(
            method = "thread/queue/add",
            paramsSerializer = ThreadQueueAddParams.serializer(),
            resultSerializer = ThreadQueueSubmissionResult.serializer(),
            requiresExperimentalApi = true,
        )

        /** Lists queued submissions for a thread. */
        val List: CodexRpcMethod<ThreadQueueListParams, ThreadQueueListResult> = CodexRpcMethod(
            method = "thread/queue/list",
            paramsSerializer = ThreadQueueListParams.serializer(),
            resultSerializer = ThreadQueueListResult.serializer(),
            requiresExperimentalApi = true,
        )

        /** Updates a queued submission. */
        val Update: CodexRpcMethod<ThreadQueueUpdateParams, ThreadQueueSubmissionResult> = CodexRpcMethod(
            method = "thread/queue/update",
            paramsSerializer = ThreadQueueUpdateParams.serializer(),
            resultSerializer = ThreadQueueSubmissionResult.serializer(),
            requiresExperimentalApi = true,
        )

        /** Deletes a queued submission. */
        val Delete: CodexRpcMethod<ThreadQueueDeleteParams, ThreadQueueDeleteResult> = CodexRpcMethod(
            method = "thread/queue/delete",
            paramsSerializer = ThreadQueueDeleteParams.serializer(),
            resultSerializer = ThreadQueueDeleteResult.serializer(),
            requiresExperimentalApi = true,
        )

        /** Changes the ordering of queued submissions. */
        val Reorder: CodexRpcMethod<ThreadQueueReorderParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/queue/reorder",
            paramsSerializer = ThreadQueueReorderParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            requiresExperimentalApi = true,
            emptyResult = CodexRpcUnit,
        )

        /** Requests execution of a queued submission. */
        val Start: CodexRpcMethod<ThreadQueueStartParams, ThreadQueueStartResult> = CodexRpcMethod(
            method = "thread/queue/start",
            paramsSerializer = ThreadQueueStartParams.serializer(),
            resultSerializer = ThreadQueueStartResult.serializer(),
            requiresExperimentalApi = true,
        )

    }

    /** Experimental local user verification and request-ID cancellation operations. */
    @ExperimentalCodexApi
    object UserVerification {
        /** Reads the local verification status. */
        val Status: CodexRpcMethod<CodexRpcUnit, UserVerificationStatusResult> = CodexRpcMethod(
            method = "userVerification/status",
            paramsSerializer = CodexRpcUnit.serializer(),
            resultSerializer = UserVerificationStatusResult.serializer(),
            requiresExperimentalApi = true,
        )

        /** Starts local verification enrollment. */
        val Enroll: CodexRpcMethod<CodexRpcUnit, UserVerificationEnrollResult> = CodexRpcMethod(
            method = "userVerification/enroll",
            paramsSerializer = CodexRpcUnit.serializer(),
            resultSerializer = UserVerificationEnrollResult.serializer(),
            requiresExperimentalApi = true,
        )

        /** Deletes the local verification enrollment. */
        val Delete: CodexRpcMethod<CodexRpcUnit, CodexRpcUnit> = CodexRpcMethod(
            method = "userVerification/delete",
            paramsSerializer = CodexRpcUnit.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            requiresExperimentalApi = true,
            emptyResult = CodexRpcUnit,
        )

        /** Requests local user verification. */
        val Verify: CodexRpcMethod<UserVerificationVerifyParams, UserVerificationVerifyResult> = CodexRpcMethod(
            method = "userVerification/verify",
            paramsSerializer = UserVerificationVerifyParams.serializer(),
            resultSerializer = UserVerificationVerifyResult.serializer(),
            requiresExperimentalApi = true,
        )

        /** Cancels a verification request by its outbound request ID. */
        val Cancel: CodexRpcMethod<UserVerificationCancelParams, CodexRpcUnit> = CodexRpcMethod(
            method = "userVerification/cancel",
            paramsSerializer = UserVerificationCancelParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            requiresExperimentalApi = true,
            emptyResult = CodexRpcUnit,
        )

    }


    /** Thread lifecycle, metadata, goals, and paged history operations. */
    object Thread {
        /** Creates a thread and returns its initial configuration. */
        val Start: CodexRpcMethod<ThreadStartParams, ThreadStartResult> = CodexRpcMethod(
            method = "thread/start",
            paramsSerializer = ThreadStartParams.serializer(),
            resultSerializer = ThreadStartResult.serializer(),
        )

        /** Resumes a thread with optional configuration overrides. */
        val Resume: CodexRpcMethod<ThreadResumeParams, ThreadResumeResult> = CodexRpcMethod(
            method = "thread/resume",
            paramsSerializer = ThreadResumeParams.serializer(),
            resultSerializer = ThreadResumeResult.serializer(),
        )

        /** Creates a new thread from an existing history. */
        val Fork: CodexRpcMethod<ThreadForkParams, ThreadForkResult> = CodexRpcMethod(
            method = "thread/fork",
            paramsSerializer = ThreadForkParams.serializer(),
            resultSerializer = ThreadForkResult.serializer(),
        )

        /** Lists a filtered page of stored threads. */
        val List: CodexRpcMethod<ThreadListParams, ThreadListResult> = CodexRpcMethod(
            method = "thread/list",
            paramsSerializer = ThreadListParams.serializer(),
            resultSerializer = ThreadListResult.serializer(),
        )

        /** Lists a page of thread IDs loaded by this app-server. */
        val ListLoaded: CodexRpcMethod<ThreadLoadedListParams, ThreadLoadedListResult> = CodexRpcMethod(
            method = "thread/loaded/list",
            paramsSerializer = ThreadLoadedListParams.serializer(),
            resultSerializer = ThreadLoadedListResult.serializer(),
        )

        /** Reads thread metadata and optionally its turns. */
        val Read: CodexRpcMethod<ThreadReadParams, ThreadReadResult> = CodexRpcMethod(
            method = "thread/read",
            paramsSerializer = ThreadReadParams.serializer(),
            resultSerializer = ThreadReadResult.serializer(),
        )

        /** Archives a thread. */
        val Archive: CodexRpcMethod<ThreadArchiveParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/archive",
            paramsSerializer = ThreadArchiveParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        /** Restores an archived thread. */
        val Unarchive: CodexRpcMethod<ThreadUnarchiveParams, ThreadUnarchiveResult> = CodexRpcMethod(
            method = "thread/unarchive",
            paramsSerializer = ThreadUnarchiveParams.serializer(),
            resultSerializer = ThreadUnarchiveResult.serializer(),
        )

        /** Deletes a thread through the server lifecycle API. */
        val Delete: CodexRpcMethod<ThreadDeleteParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/delete",
            paramsSerializer = ThreadDeleteParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        /** Detaches this connection from a thread subscription. */
        val Unsubscribe: CodexRpcMethod<ThreadUnsubscribeParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/unsubscribe",
            paramsSerializer = ThreadUnsubscribeParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        /** Sets the thread display name. */
        val SetName: CodexRpcMethod<ThreadSetNameParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/name/set",
            paramsSerializer = ThreadSetNameParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        /** Applies a thread metadata patch. */
        val UpdateMetadata: CodexRpcMethod<ThreadMetadataUpdateParams, ThreadMetadataUpdateResult> = CodexRpcMethod(
            method = "thread/metadata/update",
            paramsSerializer = ThreadMetadataUpdateParams.serializer(),
            resultSerializer = ThreadMetadataUpdateResult.serializer(),
        )

        /** Sets a thread objective and optional token budget. */
        val SetGoal: CodexRpcMethod<ThreadGoalSetParams, ThreadGoalSetResult> = CodexRpcMethod(
            method = "thread/goal/set",
            paramsSerializer = ThreadGoalSetParams.serializer(),
            resultSerializer = ThreadGoalSetResult.serializer(),
        )

        /** Reads the current thread goal. */
        val GetGoal: CodexRpcMethod<ThreadGoalGetParams, ThreadGoalGetResult> = CodexRpcMethod(
            method = "thread/goal/get",
            paramsSerializer = ThreadGoalGetParams.serializer(),
            resultSerializer = ThreadGoalGetResult.serializer(),
        )

        /** Clears the thread goal. */
        val ClearGoal: CodexRpcMethod<ThreadGoalClearParams, ThreadGoalClearResult> = CodexRpcMethod(
            method = "thread/goal/clear",
            paramsSerializer = ThreadGoalClearParams.serializer(),
            resultSerializer = ThreadGoalClearResult.serializer(),
        )

        /** Starts history compaction; completion is reported through notifications. */
        val StartCompaction: CodexRpcMethod<ThreadCompactionStartParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/compact/start",
            paramsSerializer = ThreadCompactionStartParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        /** Reads a page of thread turns. */
        val ListTurns: CodexRpcMethod<ThreadTurnsListParams, ThreadTurnsListResult> = CodexRpcMethod(
            method = "thread/turns/list",
            paramsSerializer = ThreadTurnsListParams.serializer(),
            resultSerializer = ThreadTurnsListResult.serializer(),
        )

        /** Reads a page of items using an opaque or item-anchor cursor. */
        val ListItems: CodexRpcMethod<ThreadItemsListParams, ThreadItemsListResult> = CodexRpcMethod(
            method = "thread/items/list",
            paramsSerializer = ThreadItemsListParams.serializer(),
            resultSerializer = ThreadItemsListResult.serializer(),
        )

        /** Reverts conversation history to before the requested turn; filesystem changes are not reverted. */
        val Revert: CodexRpcMethod<ThreadRevertParams, ThreadRevertResult> = CodexRpcMethod(
            method = "thread/revert",
            paramsSerializer = ThreadRevertParams.serializer(),
            resultSerializer = ThreadRevertResult.serializer(),
        )

        /** Moves a thread into the requested section. */
        val MoveToSection: CodexRpcMethod<ThreadSectionMoveParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/section/move",
            paramsSerializer = ThreadSectionMoveParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
        )
    }

    /** Operations for organizing threads into named sections. */
    object ThreadSection {
        /** Lists available thread sections. */
        val List: CodexRpcMethod<ThreadSectionListParams, ThreadSectionListResult> = CodexRpcMethod(
            method = "threadSection/list",
            paramsSerializer = ThreadSectionListParams.serializer(),
            resultSerializer = ThreadSectionListResult.serializer(),
        )

        /** Creates a named thread section. */
        val Create: CodexRpcMethod<ThreadSectionCreateParams, ThreadSectionResult> = CodexRpcMethod(
            method = "threadSection/create",
            paramsSerializer = ThreadSectionCreateParams.serializer(),
            resultSerializer = ThreadSectionResult.serializer(),
        )

        /** Updates a thread section. */
        val Update: CodexRpcMethod<ThreadSectionUpdateParams, ThreadSectionResult> = CodexRpcMethod(
            method = "threadSection/update",
            paramsSerializer = ThreadSectionUpdateParams.serializer(),
            resultSerializer = ThreadSectionResult.serializer(),
        )

        /** Deletes a thread section. */
        val Delete: CodexRpcMethod<ThreadSectionDeleteParams, CodexRpcUnit> = CodexRpcMethod(
            method = "threadSection/delete",
            paramsSerializer = ThreadSectionDeleteParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
        )
    }

    /** Operations for attaching server-managed resources to threads. */
    object ThreadAttachment {
        /** Adds an attachment to a thread. */
        val Add: CodexRpcMethod<ThreadAttachmentAddParams, ThreadAttachmentAddResult> = CodexRpcMethod(
            method = "thread/attachment/add",
            paramsSerializer = ThreadAttachmentAddParams.serializer(),
            resultSerializer = ThreadAttachmentAddResult.serializer(),
        )
        /** Lists thread attachments. */
        val List: CodexRpcMethod<ThreadAttachmentListParams, ThreadAttachmentListResult> = CodexRpcMethod(
            method = "thread/attachment/list",
            paramsSerializer = ThreadAttachmentListParams.serializer(),
            resultSerializer = ThreadAttachmentListResult.serializer(),
        )
        /** Removes an attachment from a thread. */
        val Remove: CodexRpcMethod<ThreadAttachmentRemoveParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/attachment/remove",
            paramsSerializer = ThreadAttachmentRemoveParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
        )
    }

    /** Explicit gateway OAuth state and login operations. */
    object GatewayOAuth {
        /** Reads gateway OAuth state. */
        val Read: CodexRpcMethod<CodexRpcUnit, GatewayOAuthReadResult> = CodexRpcMethod(
            method = "account/gatewayOAuth/read",
            paramsSerializer = null,
            resultSerializer = GatewayOAuthReadResult.serializer(),
        )
        /** Starts explicit gateway OAuth login. */
        val Login: CodexRpcMethod<CodexRpcUnit, CodexRpcUnit> = CodexRpcMethod(
            method = "account/gatewayOAuth/login",
            paramsSerializer = null,
            resultSerializer = CodexRpcUnit.serializer(),
        )
        /** Cancels the explicit gateway OAuth flow. */
        val Cancel: CodexRpcMethod<CodexRpcUnit, CodexRpcUnit> = CodexRpcMethod(
            method = "account/gatewayOAuth/cancel",
            paramsSerializer = null,
            resultSerializer = CodexRpcUnit.serializer(),
        )
    }

    /** Turn execution operations; completion and item updates arrive as notifications. */
    object Turn {
        /** Starts a turn; subsequent execution is streamed as notifications. */
        val Start: CodexRpcMethod<TurnStartParams, TurnStartResult> = CodexRpcMethod(
            method = "turn/start",
            paramsSerializer = TurnStartParams.serializer(),
            resultSerializer = TurnStartResult.serializer(),
        )

        /** Adds input to the expected active turn. */
        val Steer: CodexRpcMethod<TurnSteerParams, TurnSteerResult> = CodexRpcMethod(
            method = "turn/steer",
            paramsSerializer = TurnSteerParams.serializer(),
            resultSerializer = TurnSteerResult.serializer(),
        )

        /** Requests interruption of a turn. */
        val Interrupt: CodexRpcMethod<TurnInterruptParams, CodexRpcUnit> = CodexRpcMethod(
            method = "turn/interrupt",
            paramsSerializer = TurnInterruptParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )
    }

    /** Command execution under the server-selected or supplied sandbox policy, and interactive control. */
    object Command {
        /** Executes a command under the supplied server sandbox policy. */
        val Exec: CodexRpcMethod<CommandExecParams, CommandExecResult> = CodexRpcMethod(
            method = "command/exec",
            paramsSerializer = CommandExecParams.serializer(),
            resultSerializer = CommandExecResult.serializer(),
        )

        /** Writes to or closes the stdin of an interactive command. */
        val WriteStdin: CodexRpcMethod<CommandExecWriteParams, CodexRpcUnit> = CodexRpcMethod(
            method = "command/exec/write",
            paramsSerializer = CommandExecWriteParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        /** Resizes an interactive command terminal. */
        val Resize: CodexRpcMethod<CommandExecResizeParams, CodexRpcUnit> = CodexRpcMethod(
            method = "command/exec/resize",
            paramsSerializer = CommandExecResizeParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        /** Terminates an interactive command. */
        val Terminate: CodexRpcMethod<CommandExecTerminateParams, CodexRpcUnit> = CodexRpcMethod(
            method = "command/exec/terminate",
            paramsSerializer = CommandExecTerminateParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )
    }

    /** Filesystem operations on the app-server host; server permissions govern access. */
    object Filesystem {
        /** Reads file contents from the app-server host. */
        val ReadFile: CodexRpcMethod<FilesystemReadFileParams, FilesystemReadFileResult> = CodexRpcMethod(
            method = "fs/readFile",
            paramsSerializer = FilesystemReadFileParams.serializer(),
            resultSerializer = FilesystemReadFileResult.serializer(),
        )

        /** Reads host filesystem metadata. */
        val GetMetadata: CodexRpcMethod<FilesystemGetMetadataParams, FilesystemGetMetadataResult> = CodexRpcMethod(
            method = "fs/getMetadata",
            paramsSerializer = FilesystemGetMetadataParams.serializer(),
            resultSerializer = FilesystemGetMetadataResult.serializer(),
        )

        /** Lists entries in a host directory. */
        val ReadDirectory: CodexRpcMethod<FilesystemReadDirectoryParams, FilesystemReadDirectoryResult> =
            CodexRpcMethod(
                method = "fs/readDirectory",
                paramsSerializer = FilesystemReadDirectoryParams.serializer(),
                resultSerializer = FilesystemReadDirectoryResult.serializer(),
            )

        /** Writes file contents on the host. */
        val WriteFile: CodexRpcMethod<FilesystemWriteFileParams, CodexRpcUnit> = CodexRpcMethod(
            method = "fs/writeFile",
            paramsSerializer = FilesystemWriteFileParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        /** Creates a host directory. */
        val CreateDirectory: CodexRpcMethod<FilesystemCreateDirectoryParams, CodexRpcUnit> = CodexRpcMethod(
            method = "fs/createDirectory",
            paramsSerializer = FilesystemCreateDirectoryParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        /** Copies a host filesystem entry. */
        val Copy: CodexRpcMethod<FilesystemCopyParams, CodexRpcUnit> = CodexRpcMethod(
            method = "fs/copy",
            paramsSerializer = FilesystemCopyParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        /** Removes a host filesystem entry. */
        val Remove: CodexRpcMethod<FilesystemRemoveParams, CodexRpcUnit> = CodexRpcMethod(
            method = "fs/remove",
            paramsSerializer = FilesystemRemoveParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        /** Registers a filesystem watch; changes arrive as notifications. */
        val Watch: CodexRpcMethod<FilesystemWatchParams, FilesystemWatchResult> = CodexRpcMethod(
            method = "fs/watch",
            paramsSerializer = FilesystemWatchParams.serializer(),
            resultSerializer = FilesystemWatchResult.serializer(),
        )

        /** Removes a filesystem watch. */
        val Unwatch: CodexRpcMethod<FilesystemUnwatchParams, CodexRpcUnit> = CodexRpcMethod(
            method = "fs/unwatch",
            paramsSerializer = FilesystemUnwatchParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )
    }

    /** Experimental unsandboxed host process control; use only with a trusted app-server. */
    @ExperimentalCodexApi
    object Process {
        /**
         * Starts an unsandboxed host process and returns only an acknowledgement.
         *
         * Typed routing for `process/outputDelta` and `process/exited` is deferred. These events
         * become [CodexNotification.Unknown], so output and exit details are unavailable through
         * [CodexClient.notifications].
         */
        val Spawn: CodexRpcMethod<ProcessSpawnParams, CodexRpcUnit> = CodexRpcMethod(
            method = "process/spawn",
            paramsSerializer = ProcessSpawnParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        /** Writes to or closes process stdin. */
        val WriteStdin: CodexRpcMethod<ProcessWriteStdinParams, CodexRpcUnit> = CodexRpcMethod(
            method = "process/writeStdin",
            paramsSerializer = ProcessWriteStdinParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        /** Kills an unsandboxed host process. */
        val Kill: CodexRpcMethod<ProcessKillParams, CodexRpcUnit> = CodexRpcMethod(
            method = "process/kill",
            paramsSerializer = ProcessKillParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        /** Resizes a process terminal. */
        val ResizePty: CodexRpcMethod<ProcessResizePtyParams, CodexRpcUnit> = CodexRpcMethod(
            method = "process/resizePty",
            paramsSerializer = ProcessResizePtyParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )
    }

    /** Server-side review execution, with results streamed as turn events. */
    object Review {
        /** Starts a review and returns the review turn. */
        val Start: CodexRpcMethod<ReviewStartParams, ReviewStartResult> = CodexRpcMethod(
            method = "review/start",
            paramsSerializer = ReviewStartParams.serializer(),
            resultSerializer = ReviewStartResult.serializer(),
        )
    }

    /** Available model catalog and provider capability discovery. */
    object Model {
        /** Lists a page of available models. */
        val List: CodexRpcMethod<ModelListParams, ModelListResult> = CodexRpcMethod(
            method = "model/list",
            paramsSerializer = ModelListParams.serializer(),
            resultSerializer = ModelListResult.serializer(),
        )

        /** Reads provider capabilities. */
        val ReadProviderCapabilities:
            CodexRpcMethod<ModelProviderCapabilitiesReadParams, ModelProviderCapabilities> =
            CodexRpcMethod(
                method = "modelProvider/capabilities/read",
                paramsSerializer = ModelProviderCapabilitiesReadParams.serializer(),
                resultSerializer = ModelProviderCapabilities.serializer(),
            )
    }

    /** Effective configuration, persisted writes, and managed requirements. */
    object Config {
        /** Reads effective configuration and optional layer metadata. */
        val Read: CodexRpcMethod<ConfigReadParams, ConfigReadResult> = CodexRpcMethod(
            method = "config/read",
            paramsSerializer = ConfigReadParams.serializer(),
            resultSerializer = ConfigReadResult.serializer(),
        )

        /** Persists one configuration value. */
        val WriteValue: CodexRpcMethod<ConfigValueWriteParams, ConfigWriteResult> = CodexRpcMethod(
            method = "config/value/write",
            paramsSerializer = ConfigValueWriteParams.serializer(),
            resultSerializer = ConfigWriteResult.serializer(),
        )

        /** Persists a batch of configuration edits. */
        val BatchWrite: CodexRpcMethod<ConfigBatchWriteParams, ConfigWriteResult> = CodexRpcMethod(
            method = "config/batchWrite",
            paramsSerializer = ConfigBatchWriteParams.serializer(),
            resultSerializer = ConfigWriteResult.serializer(),
        )

        /** Reads managed configuration requirements. */
        val ReadRequirements: CodexRpcMethod<ManagedPolicyReadParams, ManagedPolicyReadResult> =
            CodexRpcMethod(
                method = "configRequirements/read",
                paramsSerializer = null,
                resultSerializer = ManagedPolicyReadResult.serializer(),
            )
    }

    /** Skill discovery, search roots, and enablement configuration. */
    object Skills {
        /** Lists skills for the requested working directories. */
        val List: CodexRpcMethod<SkillsListParams, SkillsListResult> = CodexRpcMethod(
            method = "skills/list",
            paramsSerializer = SkillsListParams.serializer(),
            resultSerializer = SkillsListResult.serializer(),
        )

        /** Sets additional skill search roots. */
        val SetExtraRoots: CodexRpcMethod<SkillsExtraRootsSetParams, CodexRpcUnit> =
            CodexRpcMethod(
                method = "skills/extraRoots/set",
                paramsSerializer = SkillsExtraRootsSetParams.serializer(),
                resultSerializer = CodexRpcUnit.serializer(),
                emptyResult = CodexRpcUnit,
            )

        /** Changes skill enablement. */
        val WriteConfig: CodexRpcMethod<SkillConfigWriteParams, SkillConfigWriteResult> =
            CodexRpcMethod(
                method = "skills/config/write",
                paramsSerializer = SkillConfigWriteParams.serializer(),
                resultSerializer = SkillConfigWriteResult.serializer(),
            )
    }

    /** Hook discovery for the requested working directories. */
    object Hooks {
        /** Lists hooks for the requested working directories. */
        val List: CodexRpcMethod<HooksListParams, HooksListResult> = CodexRpcMethod(
            method = "hooks/list",
            paramsSerializer = HooksListParams.serializer(),
            resultSerializer = HooksListResult.serializer(),
        )
    }

    /** Installed app runtime information. */
    object App {
        /** Reads the installed app runtime snapshot. */
        val Installed: CodexRpcMethod<AppsInstalledParams, AppsInstalledResult> = CodexRpcMethod(
            method = "app/installed",
            paramsSerializer = AppsInstalledParams.serializer(),
            resultSerializer = AppsInstalledResult.serializer(),
        )
    }

    /** Experimental app catalog discovery. */
    @ExperimentalCodexApi
    object Apps {
        /** Lists a page of available apps. */
        val List: CodexRpcMethod<AppsListParams, AppsListResult> = CodexRpcMethod(
            method = "app/list",
            paramsSerializer = AppsListParams.serializer(),
            resultSerializer = AppsListResult.serializer(),
        )
    }

    /** Marketplace registration, removal, and upgrade operations. */
    object Marketplace {
        /** Registers a marketplace. */
        val Add: CodexRpcMethod<MarketplaceAddParams, MarketplaceAddResult> = CodexRpcMethod(
            method = "marketplace/add",
            paramsSerializer = MarketplaceAddParams.serializer(),
            resultSerializer = MarketplaceAddResult.serializer(),
        )

        /** Removes a marketplace. */
        val Remove: CodexRpcMethod<MarketplaceRemoveParams, MarketplaceRemoveResult> = CodexRpcMethod(
            method = "marketplace/remove",
            paramsSerializer = MarketplaceRemoveParams.serializer(),
            resultSerializer = MarketplaceRemoveResult.serializer(),
        )

        /** Upgrades a marketplace. */
        val Upgrade: CodexRpcMethod<MarketplaceUpgradeParams, MarketplaceUpgradeResult> = CodexRpcMethod(
            method = "marketplace/upgrade",
            paramsSerializer = MarketplaceUpgradeParams.serializer(),
            resultSerializer = MarketplaceUpgradeResult.serializer(),
        )
    }

    /** Plugin discovery, content inspection, installation, and removal. */
    object Plugin {
        /** Lists plugins in marketplaces. */
        val List: CodexRpcMethod<PluginListParams, PluginListResult> = CodexRpcMethod(
            method = "plugin/list",
            paramsSerializer = PluginListParams.serializer(),
            resultSerializer = PluginListResult.serializer(),
        )

        /** Lists installed plugins. */
        val Installed: CodexRpcMethod<PluginInstalledParams, PluginInstalledResult> = CodexRpcMethod(
            method = "plugin/installed",
            paramsSerializer = PluginInstalledParams.serializer(),
            resultSerializer = PluginInstalledResult.serializer(),
        )

        /** Reads plugin details. */
        val Read: CodexRpcMethod<PluginReadParams, PluginReadResult> = CodexRpcMethod(
            method = "plugin/read",
            paramsSerializer = PluginReadParams.serializer(),
            resultSerializer = PluginReadResult.serializer(),
        )

        /** Reads plugin skill content. */
        val ReadSkill: CodexRpcMethod<PluginSkillReadParams, PluginSkillReadResult> = CodexRpcMethod(
            method = "plugin/skill/read",
            paramsSerializer = PluginSkillReadParams.serializer(),
            resultSerializer = PluginSkillReadResult.serializer(),
        )

        /** Installs a plugin. */
        val Install: CodexRpcMethod<PluginInstallParams, PluginInstallResult> = CodexRpcMethod(
            method = "plugin/install",
            paramsSerializer = PluginInstallParams.serializer(),
            resultSerializer = PluginInstallResult.serializer(),
        )

        /** Uninstalls a plugin. */
        val Uninstall: CodexRpcMethod<PluginUninstallParams, CodexRpcUnit> = CodexRpcMethod(
            method = "plugin/uninstall",
            paramsSerializer = PluginUninstallParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )
    }

    /** MCP server discovery, OAuth, resource reads, and tool execution. */
    object Mcp {
        /** Starts OAuth login for an MCP server. */
        val StartOauthLogin: CodexRpcMethod<McpServerOauthLoginParams, McpServerOauthLoginResult> =
            CodexRpcMethod(
                method = "mcpServer/oauth/login",
                paramsSerializer = McpServerOauthLoginParams.serializer(),
                resultSerializer = McpServerOauthLoginResult.serializer(),
            )

        /** Reloads MCP server configuration. */
        val ReloadConfig: CodexRpcMethod<McpConfigReloadParams, CodexRpcUnit> = CodexRpcMethod(
            method = "config/mcpServer/reload",
            paramsSerializer = null,
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        /** Lists MCP server status and discovered inventory. */
        val ListServerStatus: CodexRpcMethod<McpServerStatusListParams, McpServerStatusListResult> =
            CodexRpcMethod(
                method = "mcpServerStatus/list",
                paramsSerializer = McpServerStatusListParams.serializer(),
                resultSerializer = McpServerStatusListResult.serializer(),
            )

        /** Reads an MCP resource. */
        val ReadResource: CodexRpcMethod<McpResourceReadParams, McpResourceReadResult> =
            CodexRpcMethod(
                method = "mcpServer/resource/read",
                paramsSerializer = McpResourceReadParams.serializer(),
                resultSerializer = McpResourceReadResult.serializer(),
            )

        /** Calls an MCP tool and preserves its result payload. */
        val CallTool: CodexRpcMethod<McpServerToolCallParams, McpServerToolCallResult> =
            CodexRpcMethod(
                method = "mcpServer/tool/call",
                paramsSerializer = McpServerToolCallParams.serializer(),
                resultSerializer = McpServerToolCallResult.serializer(),
            )
    }

    /** Account authentication, usage, rate limits, and explicit account mutations. */
    object Account {
        /** Reads account and authentication status. */
        val Read: CodexRpcMethod<AccountReadParams, AccountReadResult> = CodexRpcMethod(
            method = "account/read",
            paramsSerializer = AccountReadParams.serializer(),
            resultSerializer = AccountReadResult.serializer(),
        )

        /** Starts account login. */
        val StartLogin: CodexRpcMethod<LoginAccountParams, LoginAccountResult> = CodexRpcMethod(
            method = "account/login/start",
            paramsSerializer = LoginAccountParams.serializer(),
            resultSerializer = LoginAccountResult.serializer(),
        )

        /** Cancels the identified login attempt. */
        val CancelLogin: CodexRpcMethod<CancelLoginAccountParams, CancelLoginAccountResult> = CodexRpcMethod(
            method = "account/login/cancel",
            paramsSerializer = CancelLoginAccountParams.serializer(),
            resultSerializer = CancelLoginAccountResult.serializer(),
        )

        /** Logs out the account. */
        val Logout: CodexRpcMethod<LogoutAccountParams, CodexRpcUnit> = CodexRpcMethod(
            method = "account/logout",
            paramsSerializer = null,
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        /** Reads account rate-limit windows. */
        val ReadRateLimits: CodexRpcMethod<AccountRateLimitsReadParams, AccountRateLimitsResult> = CodexRpcMethod(
            method = "account/rateLimits/read",
            paramsSerializer = null,
            resultSerializer = AccountRateLimitsResult.serializer(),
        )

        /** Reads account usage. */
        val ReadUsage: CodexRpcMethod<AccountUsageReadParams, AccountUsageResult> = CodexRpcMethod(
            method = "account/usage/read",
            paramsSerializer = null,
            resultSerializer = AccountUsageResult.serializer(),
        )

        /** Reads workspace messages for the account. */
        val ReadWorkspaceMessages:
            CodexRpcMethod<AccountWorkspaceMessagesReadParams, AccountWorkspaceMessagesResult> =
            CodexRpcMethod(
                method = "account/workspaceMessages/read",
                paramsSerializer = null,
                resultSerializer = AccountWorkspaceMessagesResult.serializer(),
            )

        /** Consumes a reset credit using an explicit idempotency key. */
        val ConsumeRateLimitResetCredit:
            CodexRpcMethod<
                ConsumeAccountRateLimitResetCreditParams,
                ConsumeAccountRateLimitResetCreditResult,
                > = CodexRpcMethod(
                method = "account/rateLimitResetCredit/consume",
                paramsSerializer = ConsumeAccountRateLimitResetCreditParams.serializer(),
                resultSerializer = ConsumeAccountRateLimitResetCreditResult.serializer(),
            )

        /** Requests an add-credits nudge email. */
        val SendAddCreditsNudgeEmail:
            CodexRpcMethod<SendAddCreditsNudgeEmailParams, SendAddCreditsNudgeEmailResult> =
            CodexRpcMethod(
                method = "account/sendAddCreditsNudgeEmail",
                paramsSerializer = SendAddCreditsNudgeEmailParams.serializer(),
                resultSerializer = SendAddCreditsNudgeEmailResult.serializer(),
            )
    }

    /** Available permission profiles reported by the app-server. */
    object PermissionProfile {
        /** Lists available permission profiles. */
        val List: CodexRpcMethod<PermissionProfileListParams, PermissionProfileListResult> =
            CodexRpcMethod(
                method = "permissionProfile/list",
                paramsSerializer = PermissionProfileListParams.serializer(),
                resultSerializer = PermissionProfileListResult.serializer(),
            )
    }

    /** Experimental collaboration-mode presets. */
    @ExperimentalCodexApi
    object CollaborationMode {
        /** Lists collaboration-mode presets. */
        val List: CodexRpcMethod<CollaborationModeListParams, CollaborationModeListResult> =
            CodexRpcMethod(
                method = "collaborationMode/list",
                paramsSerializer = CollaborationModeListParams.serializer(),
                resultSerializer = CollaborationModeListResult.serializer(),
            )
    }

    /** Experimental environment registration. */
    @ExperimentalCodexApi
    object Environment {
        /** Registers an environment. */
        val Add: CodexRpcMethod<EnvironmentAddParams, CodexRpcUnit> = CodexRpcMethod(
            method = "environment/add",
            paramsSerializer = EnvironmentAddParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )
    }

    /** Experimental remote-control enablement, pairing, and client authorization management. */
    @ExperimentalCodexApi
    object RemoteControl {
        /** Enables remote control. */
        val Enable: CodexRpcMethod<RemoteControlEnableParams, RemoteControlStatusSnapshot> = CodexRpcMethod(
            method = "remoteControl/enable",
            paramsSerializer = RemoteControlEnableParams.serializer(),
            resultSerializer = RemoteControlStatusSnapshot.serializer(),
        )

        /** Disables remote control. */
        val Disable: CodexRpcMethod<RemoteControlDisableParams, RemoteControlStatusSnapshot> = CodexRpcMethod(
            method = "remoteControl/disable",
            paramsSerializer = RemoteControlDisableParams.serializer(),
            resultSerializer = RemoteControlStatusSnapshot.serializer(),
        )

        /** Reads the remote-control status snapshot. */
        val ReadStatus: CodexRpcMethod<RemoteControlStatusReadParams, RemoteControlStatusSnapshot> = CodexRpcMethod(
            method = "remoteControl/status/read",
            paramsSerializer = null,
            resultSerializer = RemoteControlStatusSnapshot.serializer(),
        )

        /** Starts a remote-control pairing session. */
        val StartPairing:
            CodexRpcMethod<RemoteControlPairingStartParams, RemoteControlPairingStartResult> =
            CodexRpcMethod(
                method = "remoteControl/pairing/start",
                paramsSerializer = RemoteControlPairingStartParams.serializer(),
                resultSerializer = RemoteControlPairingStartResult.serializer(),
            )

        /** Reads pairing status. */
        val ReadPairingStatus:
            CodexRpcMethod<RemoteControlPairingStatusParams, RemoteControlPairingStatusResult> =
            CodexRpcMethod(
                method = "remoteControl/pairing/status",
                paramsSerializer = RemoteControlPairingStatusParams.serializer(),
                resultSerializer = RemoteControlPairingStatusResult.serializer(),
            )

        /** Lists paired remote-control clients. */
        val ListClients: CodexRpcMethod<RemoteControlClientsListParams, RemoteControlClientsListResult> =
            CodexRpcMethod(
                method = "remoteControl/client/list",
                paramsSerializer = RemoteControlClientsListParams.serializer(),
                resultSerializer = RemoteControlClientsListResult.serializer(),
            )

        /** Revokes a remote-control client. */
        val RevokeClient:
            CodexRpcMethod<RemoteControlClientsRevokeParams, RemoteControlClientsRevokeResult> =
            CodexRpcMethod(
                method = "remoteControl/client/revoke",
                paramsSerializer = RemoteControlClientsRevokeParams.serializer(),
                resultSerializer = RemoteControlClientsRevokeResult.serializer(),
            )
    }
}
