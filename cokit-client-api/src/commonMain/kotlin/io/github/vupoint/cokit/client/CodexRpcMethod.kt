package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.auth.AccountReadParams
import io.github.vupoint.cokit.client.auth.AccountReadResult
import io.github.vupoint.cokit.client.auth.AccountRateLimitsReadParams
import io.github.vupoint.cokit.client.auth.AccountRateLimitsResult
import io.github.vupoint.cokit.client.auth.AccountUsageReadParams
import io.github.vupoint.cokit.client.auth.AccountUsageResult
import io.github.vupoint.cokit.client.auth.CancelLoginAccountParams
import io.github.vupoint.cokit.client.auth.CancelLoginAccountResult
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
import kotlinx.serialization.KSerializer

class CodexRpcMethod<P : Any, R : Any> internal constructor(
    val method: String,
    val paramsSerializer: KSerializer<P>?,
    val resultSerializer: KSerializer<R>,
    val emptyResult: R? = null,
)

object CodexRpc {
    object Thread {
        val Start: CodexRpcMethod<ThreadStartParams, ThreadStartResult> = CodexRpcMethod(
            method = "thread/start",
            paramsSerializer = ThreadStartParams.serializer(),
            resultSerializer = ThreadStartResult.serializer(),
        )

        val Resume: CodexRpcMethod<ThreadResumeParams, ThreadResumeResult> = CodexRpcMethod(
            method = "thread/resume",
            paramsSerializer = ThreadResumeParams.serializer(),
            resultSerializer = ThreadResumeResult.serializer(),
        )

        val Fork: CodexRpcMethod<ThreadForkParams, ThreadForkResult> = CodexRpcMethod(
            method = "thread/fork",
            paramsSerializer = ThreadForkParams.serializer(),
            resultSerializer = ThreadForkResult.serializer(),
        )

        val List: CodexRpcMethod<ThreadListParams, ThreadListResult> = CodexRpcMethod(
            method = "thread/list",
            paramsSerializer = ThreadListParams.serializer(),
            resultSerializer = ThreadListResult.serializer(),
        )

        val Read: CodexRpcMethod<ThreadReadParams, ThreadReadResult> = CodexRpcMethod(
            method = "thread/read",
            paramsSerializer = ThreadReadParams.serializer(),
            resultSerializer = ThreadReadResult.serializer(),
        )

        val Archive: CodexRpcMethod<ThreadArchiveParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/archive",
            paramsSerializer = ThreadArchiveParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Unarchive: CodexRpcMethod<ThreadUnarchiveParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/unarchive",
            paramsSerializer = ThreadUnarchiveParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Delete: CodexRpcMethod<ThreadDeleteParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/delete",
            paramsSerializer = ThreadDeleteParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Unsubscribe: CodexRpcMethod<ThreadUnsubscribeParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/unsubscribe",
            paramsSerializer = ThreadUnsubscribeParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val SetName: CodexRpcMethod<ThreadSetNameParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/name/set",
            paramsSerializer = ThreadSetNameParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val UpdateMetadata: CodexRpcMethod<ThreadMetadataUpdateParams, ThreadMetadataUpdateResult> = CodexRpcMethod(
            method = "thread/metadata/update",
            paramsSerializer = ThreadMetadataUpdateParams.serializer(),
            resultSerializer = ThreadMetadataUpdateResult.serializer(),
        )

        val SetGoal: CodexRpcMethod<ThreadGoalSetParams, ThreadGoalSetResult> = CodexRpcMethod(
            method = "thread/goal/set",
            paramsSerializer = ThreadGoalSetParams.serializer(),
            resultSerializer = ThreadGoalSetResult.serializer(),
        )

        val GetGoal: CodexRpcMethod<ThreadGoalGetParams, ThreadGoalGetResult> = CodexRpcMethod(
            method = "thread/goal/get",
            paramsSerializer = ThreadGoalGetParams.serializer(),
            resultSerializer = ThreadGoalGetResult.serializer(),
        )

        val ClearGoal: CodexRpcMethod<ThreadGoalClearParams, ThreadGoalClearResult> = CodexRpcMethod(
            method = "thread/goal/clear",
            paramsSerializer = ThreadGoalClearParams.serializer(),
            resultSerializer = ThreadGoalClearResult.serializer(),
        )

        val StartCompaction: CodexRpcMethod<ThreadCompactionStartParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/compact/start",
            paramsSerializer = ThreadCompactionStartParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        @ExperimentalCodexApi
        val ListTurns: CodexRpcMethod<ThreadTurnsListParams, ThreadTurnsListResult> = CodexRpcMethod(
            method = "thread/turns/list",
            paramsSerializer = ThreadTurnsListParams.serializer(),
            resultSerializer = ThreadTurnsListResult.serializer(),
        )
    }

    object Turn {
        val Start: CodexRpcMethod<TurnStartParams, TurnStartResult> = CodexRpcMethod(
            method = "turn/start",
            paramsSerializer = TurnStartParams.serializer(),
            resultSerializer = TurnStartResult.serializer(),
        )

        val Steer: CodexRpcMethod<TurnSteerParams, CodexRpcUnit> = CodexRpcMethod(
            method = "turn/steer",
            paramsSerializer = TurnSteerParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Interrupt: CodexRpcMethod<TurnInterruptParams, CodexRpcUnit> = CodexRpcMethod(
            method = "turn/interrupt",
            paramsSerializer = TurnInterruptParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )
    }

    object Command {
        val Exec: CodexRpcMethod<CommandExecParams, CommandExecResult> = CodexRpcMethod(
            method = "command/exec",
            paramsSerializer = CommandExecParams.serializer(),
            resultSerializer = CommandExecResult.serializer(),
        )

        val WriteStdin: CodexRpcMethod<CommandExecWriteParams, CodexRpcUnit> = CodexRpcMethod(
            method = "command/exec/write",
            paramsSerializer = CommandExecWriteParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Resize: CodexRpcMethod<CommandExecResizeParams, CodexRpcUnit> = CodexRpcMethod(
            method = "command/exec/resize",
            paramsSerializer = CommandExecResizeParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Terminate: CodexRpcMethod<CommandExecTerminateParams, CodexRpcUnit> = CodexRpcMethod(
            method = "command/exec/terminate",
            paramsSerializer = CommandExecTerminateParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )
    }

    object Filesystem {
        val ReadFile: CodexRpcMethod<FilesystemReadFileParams, FilesystemReadFileResult> = CodexRpcMethod(
            method = "fs/readFile",
            paramsSerializer = FilesystemReadFileParams.serializer(),
            resultSerializer = FilesystemReadFileResult.serializer(),
        )

        val GetMetadata: CodexRpcMethod<FilesystemGetMetadataParams, FilesystemGetMetadataResult> = CodexRpcMethod(
            method = "fs/getMetadata",
            paramsSerializer = FilesystemGetMetadataParams.serializer(),
            resultSerializer = FilesystemGetMetadataResult.serializer(),
        )

        val ReadDirectory: CodexRpcMethod<FilesystemReadDirectoryParams, FilesystemReadDirectoryResult> =
            CodexRpcMethod(
                method = "fs/readDirectory",
                paramsSerializer = FilesystemReadDirectoryParams.serializer(),
                resultSerializer = FilesystemReadDirectoryResult.serializer(),
            )

        val WriteFile: CodexRpcMethod<FilesystemWriteFileParams, CodexRpcUnit> = CodexRpcMethod(
            method = "fs/writeFile",
            paramsSerializer = FilesystemWriteFileParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val CreateDirectory: CodexRpcMethod<FilesystemCreateDirectoryParams, CodexRpcUnit> = CodexRpcMethod(
            method = "fs/createDirectory",
            paramsSerializer = FilesystemCreateDirectoryParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Copy: CodexRpcMethod<FilesystemCopyParams, CodexRpcUnit> = CodexRpcMethod(
            method = "fs/copy",
            paramsSerializer = FilesystemCopyParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Remove: CodexRpcMethod<FilesystemRemoveParams, CodexRpcUnit> = CodexRpcMethod(
            method = "fs/remove",
            paramsSerializer = FilesystemRemoveParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Watch: CodexRpcMethod<FilesystemWatchParams, FilesystemWatchResult> = CodexRpcMethod(
            method = "fs/watch",
            paramsSerializer = FilesystemWatchParams.serializer(),
            resultSerializer = FilesystemWatchResult.serializer(),
        )

        val Unwatch: CodexRpcMethod<FilesystemUnwatchParams, CodexRpcUnit> = CodexRpcMethod(
            method = "fs/unwatch",
            paramsSerializer = FilesystemUnwatchParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )
    }

    @ExperimentalCodexApi
    object Process {
        val Spawn: CodexRpcMethod<ProcessSpawnParams, CodexRpcUnit> = CodexRpcMethod(
            method = "process/spawn",
            paramsSerializer = ProcessSpawnParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val WriteStdin: CodexRpcMethod<ProcessWriteStdinParams, CodexRpcUnit> = CodexRpcMethod(
            method = "process/writeStdin",
            paramsSerializer = ProcessWriteStdinParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Kill: CodexRpcMethod<ProcessKillParams, CodexRpcUnit> = CodexRpcMethod(
            method = "process/kill",
            paramsSerializer = ProcessKillParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val ResizePty: CodexRpcMethod<ProcessResizePtyParams, CodexRpcUnit> = CodexRpcMethod(
            method = "process/resizePty",
            paramsSerializer = ProcessResizePtyParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )
    }

    object Review {
        val Start: CodexRpcMethod<ReviewStartParams, ReviewStartResult> = CodexRpcMethod(
            method = "review/start",
            paramsSerializer = ReviewStartParams.serializer(),
            resultSerializer = ReviewStartResult.serializer(),
        )
    }

    object Model {
        val List: CodexRpcMethod<ModelListParams, ModelListResult> = CodexRpcMethod(
            method = "model/list",
            paramsSerializer = ModelListParams.serializer(),
            resultSerializer = ModelListResult.serializer(),
        )

        val ReadProviderCapabilities:
            CodexRpcMethod<ModelProviderCapabilitiesReadParams, ModelProviderCapabilities> =
            CodexRpcMethod(
                method = "modelProvider/capabilities/read",
                paramsSerializer = ModelProviderCapabilitiesReadParams.serializer(),
                resultSerializer = ModelProviderCapabilities.serializer(),
            )
    }

    object Config {
        val Read: CodexRpcMethod<ConfigReadParams, ConfigReadResult> = CodexRpcMethod(
            method = "config/read",
            paramsSerializer = ConfigReadParams.serializer(),
            resultSerializer = ConfigReadResult.serializer(),
        )

        val WriteValue: CodexRpcMethod<ConfigValueWriteParams, ConfigWriteResult> = CodexRpcMethod(
            method = "config/value/write",
            paramsSerializer = ConfigValueWriteParams.serializer(),
            resultSerializer = ConfigWriteResult.serializer(),
        )

        val BatchWrite: CodexRpcMethod<ConfigBatchWriteParams, ConfigWriteResult> = CodexRpcMethod(
            method = "config/batchWrite",
            paramsSerializer = ConfigBatchWriteParams.serializer(),
            resultSerializer = ConfigWriteResult.serializer(),
        )

        val ReadRequirements: CodexRpcMethod<ManagedPolicyReadParams, ManagedPolicyReadResult> =
            CodexRpcMethod(
                method = "configRequirements/read",
                paramsSerializer = null,
                resultSerializer = ManagedPolicyReadResult.serializer(),
            )
    }

    object Skills {
        val List: CodexRpcMethod<SkillsListParams, SkillsListResult> = CodexRpcMethod(
            method = "skills/list",
            paramsSerializer = SkillsListParams.serializer(),
            resultSerializer = SkillsListResult.serializer(),
        )

        val SetExtraRoots: CodexRpcMethod<SkillsExtraRootsSetParams, CodexRpcUnit> =
            CodexRpcMethod(
                method = "skills/extraRoots/set",
                paramsSerializer = SkillsExtraRootsSetParams.serializer(),
                resultSerializer = CodexRpcUnit.serializer(),
                emptyResult = CodexRpcUnit,
            )

        val WriteConfig: CodexRpcMethod<SkillConfigWriteParams, SkillConfigWriteResult> =
            CodexRpcMethod(
                method = "skills/config/write",
                paramsSerializer = SkillConfigWriteParams.serializer(),
                resultSerializer = SkillConfigWriteResult.serializer(),
            )
    }

    object Hooks {
        val List: CodexRpcMethod<HooksListParams, HooksListResult> = CodexRpcMethod(
            method = "hooks/list",
            paramsSerializer = HooksListParams.serializer(),
            resultSerializer = HooksListResult.serializer(),
        )
    }

    @ExperimentalCodexApi
    object Apps {
        val List: CodexRpcMethod<AppsListParams, AppsListResult> = CodexRpcMethod(
            method = "app/list",
            paramsSerializer = AppsListParams.serializer(),
            resultSerializer = AppsListResult.serializer(),
        )
    }

    object Marketplace {
        val Add: CodexRpcMethod<MarketplaceAddParams, MarketplaceAddResult> = CodexRpcMethod(
            method = "marketplace/add",
            paramsSerializer = MarketplaceAddParams.serializer(),
            resultSerializer = MarketplaceAddResult.serializer(),
        )

        val Remove: CodexRpcMethod<MarketplaceRemoveParams, MarketplaceRemoveResult> = CodexRpcMethod(
            method = "marketplace/remove",
            paramsSerializer = MarketplaceRemoveParams.serializer(),
            resultSerializer = MarketplaceRemoveResult.serializer(),
        )

        val Upgrade: CodexRpcMethod<MarketplaceUpgradeParams, MarketplaceUpgradeResult> = CodexRpcMethod(
            method = "marketplace/upgrade",
            paramsSerializer = MarketplaceUpgradeParams.serializer(),
            resultSerializer = MarketplaceUpgradeResult.serializer(),
        )
    }

    object Plugin {
        val List: CodexRpcMethod<PluginListParams, PluginListResult> = CodexRpcMethod(
            method = "plugin/list",
            paramsSerializer = PluginListParams.serializer(),
            resultSerializer = PluginListResult.serializer(),
        )

        val Installed: CodexRpcMethod<PluginInstalledParams, PluginInstalledResult> = CodexRpcMethod(
            method = "plugin/installed",
            paramsSerializer = PluginInstalledParams.serializer(),
            resultSerializer = PluginInstalledResult.serializer(),
        )

        val Read: CodexRpcMethod<PluginReadParams, PluginReadResult> = CodexRpcMethod(
            method = "plugin/read",
            paramsSerializer = PluginReadParams.serializer(),
            resultSerializer = PluginReadResult.serializer(),
        )

        val ReadSkill: CodexRpcMethod<PluginSkillReadParams, PluginSkillReadResult> = CodexRpcMethod(
            method = "plugin/skill/read",
            paramsSerializer = PluginSkillReadParams.serializer(),
            resultSerializer = PluginSkillReadResult.serializer(),
        )

        val Install: CodexRpcMethod<PluginInstallParams, PluginInstallResult> = CodexRpcMethod(
            method = "plugin/install",
            paramsSerializer = PluginInstallParams.serializer(),
            resultSerializer = PluginInstallResult.serializer(),
        )

        val Uninstall: CodexRpcMethod<PluginUninstallParams, CodexRpcUnit> = CodexRpcMethod(
            method = "plugin/uninstall",
            paramsSerializer = PluginUninstallParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )
    }

    object Mcp {
        val StartOauthLogin: CodexRpcMethod<McpServerOauthLoginParams, McpServerOauthLoginResult> =
            CodexRpcMethod(
                method = "mcpServer/oauth/login",
                paramsSerializer = McpServerOauthLoginParams.serializer(),
                resultSerializer = McpServerOauthLoginResult.serializer(),
            )

        val ReloadConfig: CodexRpcMethod<McpConfigReloadParams, CodexRpcUnit> = CodexRpcMethod(
            method = "config/mcpServer/reload",
            paramsSerializer = null,
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val ListServerStatus: CodexRpcMethod<McpServerStatusListParams, McpServerStatusListResult> =
            CodexRpcMethod(
                method = "mcpServerStatus/list",
                paramsSerializer = McpServerStatusListParams.serializer(),
                resultSerializer = McpServerStatusListResult.serializer(),
            )

        val ReadResource: CodexRpcMethod<McpResourceReadParams, McpResourceReadResult> =
            CodexRpcMethod(
                method = "mcpServer/resource/read",
                paramsSerializer = McpResourceReadParams.serializer(),
                resultSerializer = McpResourceReadResult.serializer(),
            )

        val CallTool: CodexRpcMethod<McpServerToolCallParams, McpServerToolCallResult> =
            CodexRpcMethod(
                method = "mcpServer/tool/call",
                paramsSerializer = McpServerToolCallParams.serializer(),
                resultSerializer = McpServerToolCallResult.serializer(),
            )
    }

    object Account {
        val Read: CodexRpcMethod<AccountReadParams, AccountReadResult> = CodexRpcMethod(
            method = "account/read",
            paramsSerializer = AccountReadParams.serializer(),
            resultSerializer = AccountReadResult.serializer(),
        )

        val StartLogin: CodexRpcMethod<LoginAccountParams, LoginAccountResult> = CodexRpcMethod(
            method = "account/login/start",
            paramsSerializer = LoginAccountParams.serializer(),
            resultSerializer = LoginAccountResult.serializer(),
        )

        val CancelLogin: CodexRpcMethod<CancelLoginAccountParams, CancelLoginAccountResult> = CodexRpcMethod(
            method = "account/login/cancel",
            paramsSerializer = CancelLoginAccountParams.serializer(),
            resultSerializer = CancelLoginAccountResult.serializer(),
        )

        val Logout: CodexRpcMethod<LogoutAccountParams, CodexRpcUnit> = CodexRpcMethod(
            method = "account/logout",
            paramsSerializer = null,
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val ReadRateLimits: CodexRpcMethod<AccountRateLimitsReadParams, AccountRateLimitsResult> = CodexRpcMethod(
            method = "account/rateLimits/read",
            paramsSerializer = null,
            resultSerializer = AccountRateLimitsResult.serializer(),
        )

        val ReadUsage: CodexRpcMethod<AccountUsageReadParams, AccountUsageResult> = CodexRpcMethod(
            method = "account/usage/read",
            paramsSerializer = null,
            resultSerializer = AccountUsageResult.serializer(),
        )

        val SendAddCreditsNudgeEmail:
            CodexRpcMethod<SendAddCreditsNudgeEmailParams, SendAddCreditsNudgeEmailResult> =
            CodexRpcMethod(
                method = "account/sendAddCreditsNudgeEmail",
                paramsSerializer = SendAddCreditsNudgeEmailParams.serializer(),
                resultSerializer = SendAddCreditsNudgeEmailResult.serializer(),
            )
    }

    object PermissionProfile {
        val List: CodexRpcMethod<PermissionProfileListParams, PermissionProfileListResult> =
            CodexRpcMethod(
                method = "permissionProfile/list",
                paramsSerializer = PermissionProfileListParams.serializer(),
                resultSerializer = PermissionProfileListResult.serializer(),
            )
    }

    @ExperimentalCodexApi
    object CollaborationMode {
        val List: CodexRpcMethod<CollaborationModeListParams, CollaborationModeListResult> =
            CodexRpcMethod(
                method = "collaborationMode/list",
                paramsSerializer = CollaborationModeListParams.serializer(),
                resultSerializer = CollaborationModeListResult.serializer(),
            )
    }

    @ExperimentalCodexApi
    object Environment {
        val Add: CodexRpcMethod<EnvironmentAddParams, CodexRpcUnit> = CodexRpcMethod(
            method = "environment/add",
            paramsSerializer = EnvironmentAddParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )
    }

    @ExperimentalCodexApi
    object RemoteControl {
        val Enable: CodexRpcMethod<RemoteControlEnableParams, RemoteControlStatusSnapshot> = CodexRpcMethod(
            method = "remoteControl/enable",
            paramsSerializer = RemoteControlEnableParams.serializer(),
            resultSerializer = RemoteControlStatusSnapshot.serializer(),
        )

        val Disable: CodexRpcMethod<RemoteControlDisableParams, RemoteControlStatusSnapshot> = CodexRpcMethod(
            method = "remoteControl/disable",
            paramsSerializer = RemoteControlDisableParams.serializer(),
            resultSerializer = RemoteControlStatusSnapshot.serializer(),
        )

        val ReadStatus: CodexRpcMethod<RemoteControlStatusReadParams, RemoteControlStatusSnapshot> = CodexRpcMethod(
            method = "remoteControl/status/read",
            paramsSerializer = null,
            resultSerializer = RemoteControlStatusSnapshot.serializer(),
        )

        val StartPairing:
            CodexRpcMethod<RemoteControlPairingStartParams, RemoteControlPairingStartResult> =
            CodexRpcMethod(
                method = "remoteControl/pairing/start",
                paramsSerializer = RemoteControlPairingStartParams.serializer(),
                resultSerializer = RemoteControlPairingStartResult.serializer(),
            )

        val ReadPairingStatus:
            CodexRpcMethod<RemoteControlPairingStatusParams, RemoteControlPairingStatusResult> =
            CodexRpcMethod(
                method = "remoteControl/pairing/status",
                paramsSerializer = RemoteControlPairingStatusParams.serializer(),
                resultSerializer = RemoteControlPairingStatusResult.serializer(),
            )

        val ListClients: CodexRpcMethod<RemoteControlClientsListParams, RemoteControlClientsListResult> =
            CodexRpcMethod(
                method = "remoteControl/client/list",
                paramsSerializer = RemoteControlClientsListParams.serializer(),
                resultSerializer = RemoteControlClientsListResult.serializer(),
            )

        val RevokeClient:
            CodexRpcMethod<RemoteControlClientsRevokeParams, RemoteControlClientsRevokeResult> =
            CodexRpcMethod(
                method = "remoteControl/client/revoke",
                paramsSerializer = RemoteControlClientsRevokeParams.serializer(),
                resultSerializer = RemoteControlClientsRevokeResult.serializer(),
            )
    }
}
