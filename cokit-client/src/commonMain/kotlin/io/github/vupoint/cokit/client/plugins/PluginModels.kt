package io.github.vupoint.cokit.client.plugins

import io.github.vupoint.cokit.client.CodexHostPath
import io.github.vupoint.cokit.client.extensions.AppId
import io.github.vupoint.cokit.client.extensions.HookEventName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class PluginListMarketplaceKind(val value: String) {
    companion object {
        val Local = PluginListMarketplaceKind("local")
        val Vertical = PluginListMarketplaceKind("vertical")
        val WorkspaceDirectory = PluginListMarketplaceKind("workspace-directory")
        val SharedWithMe = PluginListMarketplaceKind("shared-with-me")
    }
}

@Serializable
@JvmInline
value class PluginAuthPolicy(val value: String) {
    companion object {
        val OnInstall = PluginAuthPolicy("ON_INSTALL")
        val OnUse = PluginAuthPolicy("ON_USE")
    }
}

@Serializable
@JvmInline
value class PluginInstallPolicy(val value: String) {
    companion object {
        val NotAvailable = PluginInstallPolicy("NOT_AVAILABLE")
        val Available = PluginInstallPolicy("AVAILABLE")
        val InstalledByDefault = PluginInstallPolicy("INSTALLED_BY_DEFAULT")
    }
}

@Serializable
@JvmInline
value class PluginAvailability(val value: String) {
    companion object {
        val DisabledByAdmin = PluginAvailability("DISABLED_BY_ADMIN")
        val Available = PluginAvailability("AVAILABLE")
    }
}

@Serializable
@JvmInline
value class PluginShareDiscoverability(val value: String) {
    companion object {
        val Listed = PluginShareDiscoverability("LISTED")
        val Unlisted = PluginShareDiscoverability("UNLISTED")
        val Private = PluginShareDiscoverability("PRIVATE")
    }
}

@Serializable
@JvmInline
value class PluginSharePrincipalType(val value: String) {
    companion object {
        val User = PluginSharePrincipalType("user")
        val Group = PluginSharePrincipalType("group")
        val Workspace = PluginSharePrincipalType("workspace")
    }
}

@Serializable
@JvmInline
value class PluginSharePrincipalRole(val value: String) {
    companion object {
        val Reader = PluginSharePrincipalRole("reader")
        val Editor = PluginSharePrincipalRole("editor")
        val Owner = PluginSharePrincipalRole("owner")
    }
}

@Serializable
@JvmInline
value class AppTemplateUnavailableReason(val value: String) {
    companion object {
        val NotConfiguredForWorkspace = AppTemplateUnavailableReason("NOT_CONFIGURED_FOR_WORKSPACE")
        val NoActiveWorkspace = AppTemplateUnavailableReason("NO_ACTIVE_WORKSPACE")
    }
}

@Serializable
data class PluginListParams(
    val cwds: List<CodexHostPath>? = null,
    val marketplaceKinds: List<PluginListMarketplaceKind>? = null,
)

@Serializable
data class PluginInstalledParams(
    val cwds: List<CodexHostPath>? = null,
    val installSuggestionPluginNames: List<String>? = null,
)

@Serializable
data class PluginReadParams(
    val pluginName: String,
    val marketplacePath: CodexHostPath? = null,
    val remoteMarketplaceName: String? = null,
)

@Serializable
data class PluginInstallParams(
    val pluginName: String,
    val marketplacePath: CodexHostPath? = null,
    val remoteMarketplaceName: String? = null,
)

@Serializable
data class PluginUninstallParams(
    val pluginId: String,
)

@Serializable
data class PluginSkillReadParams(
    val remoteMarketplaceName: String,
    val remotePluginId: String,
    val skillName: String,
)

@Serializable
data class MarketplaceAddParams(
    val source: String,
    val refName: String? = null,
    val sparsePaths: List<String>? = null,
)

@Serializable
data class MarketplaceRemoveParams(
    val marketplaceName: String,
)

@Serializable
data class MarketplaceUpgradeParams(
    val marketplaceName: String? = null,
)

@Serializable
data class PluginListResult(
    val marketplaces: List<PluginMarketplaceEntry> = emptyList(),
    val featuredPluginIds: List<String> = emptyList(),
    val marketplaceLoadErrors: List<MarketplaceLoadErrorInfo> = emptyList(),
)

@Serializable
data class PluginInstalledResult(
    val marketplaces: List<PluginMarketplaceEntry> = emptyList(),
    val marketplaceLoadErrors: List<MarketplaceLoadErrorInfo> = emptyList(),
)

@Serializable
data class PluginReadResult(
    val plugin: PluginDetail,
)

@Serializable
data class PluginSkillReadResult(
    val contents: String? = null,
)

@Serializable
data class PluginInstallResult(
    val authPolicy: PluginAuthPolicy,
    val appsNeedingAuth: List<AppSummary> = emptyList(),
)

@Serializable
data class MarketplaceAddResult(
    val alreadyAdded: Boolean,
    val installedRoot: CodexHostPath,
    val marketplaceName: String,
)

@Serializable
data class MarketplaceRemoveResult(
    val marketplaceName: String,
    val installedRoot: CodexHostPath? = null,
)

@Serializable
data class MarketplaceUpgradeResult(
    val errors: List<MarketplaceUpgradeErrorInfo> = emptyList(),
    val selectedMarketplaces: List<String> = emptyList(),
    val upgradedRoots: List<CodexHostPath> = emptyList(),
)

@Serializable
data class MarketplaceUpgradeErrorInfo(
    val marketplaceName: String,
    val message: String,
)

@Serializable
data class MarketplaceLoadErrorInfo(
    val marketplacePath: CodexHostPath,
    val message: String,
)

@Serializable
data class MarketplaceInterface(
    val displayName: String? = null,
)

@Serializable
data class PluginMarketplaceEntry(
    val name: String,
    val plugins: List<PluginSummary> = emptyList(),
    @SerialName("interface")
    val interfaceMetadata: MarketplaceInterface? = null,
    val path: CodexHostPath? = null,
)

@Serializable
data class PluginSummary(
    val id: String,
    val name: String,
    val source: PluginSource,
    val installed: Boolean,
    val enabled: Boolean,
    val authPolicy: PluginAuthPolicy,
    val installPolicy: PluginInstallPolicy,
    val availability: PluginAvailability = PluginAvailability.Available,
    @SerialName("interface")
    val interfaceMetadata: PluginInterface? = null,
    val keywords: List<String> = emptyList(),
    val localVersion: String? = null,
    val remotePluginId: String? = null,
    val shareContext: PluginShareContext? = null,
)

@Serializable
sealed interface PluginSource {
    @Serializable
    @SerialName("local")
    data class Local(
        val path: CodexHostPath,
    ) : PluginSource

    @Serializable
    @SerialName("git")
    data class Git(
        val url: String,
        val path: String? = null,
        val refName: String? = null,
        val sha: String? = null,
    ) : PluginSource

    @Serializable
    @SerialName("remote")
    data object Remote : PluginSource
}

@Serializable
data class PluginInterface(
    val capabilities: List<String> = emptyList(),
    val screenshotUrls: List<String> = emptyList(),
    val screenshots: List<CodexHostPath> = emptyList(),
    val brandColor: String? = null,
    val category: String? = null,
    val composerIcon: CodexHostPath? = null,
    val composerIconUrl: String? = null,
    val defaultPrompt: List<String>? = null,
    val developerName: String? = null,
    val displayName: String? = null,
    val logo: CodexHostPath? = null,
    val logoUrl: String? = null,
    val longDescription: String? = null,
    val privacyPolicyUrl: String? = null,
    val shortDescription: String? = null,
    val termsOfServiceUrl: String? = null,
    val websiteUrl: String? = null,
)

@Serializable
data class PluginShareContext(
    val remotePluginId: String,
    val creatorAccountUserId: String? = null,
    val creatorName: String? = null,
    val discoverability: PluginShareDiscoverability? = null,
    val remoteVersion: String? = null,
    val sharePrincipals: List<PluginSharePrincipal>? = null,
    val shareUrl: String? = null,
)

@Serializable
data class PluginSharePrincipal(
    val name: String,
    val principalId: String,
    val principalType: PluginSharePrincipalType,
    val role: PluginSharePrincipalRole,
)

@Serializable
data class PluginDetail(
    val summary: PluginSummary,
    val marketplaceName: String,
    val appTemplates: List<AppTemplateSummary> = emptyList(),
    val apps: List<AppSummary> = emptyList(),
    val hooks: List<PluginHookSummary> = emptyList(),
    val mcpServers: List<String> = emptyList(),
    val skills: List<SkillSummary> = emptyList(),
    val description: String? = null,
    val marketplacePath: CodexHostPath? = null,
    val shareUrl: String? = null,
)

@Serializable
data class AppSummary(
    val id: AppId,
    val name: String,
    val category: String? = null,
    val description: String? = null,
    val installUrl: String? = null,
)

@Serializable
data class AppTemplateSummary(
    val templateId: String,
    val name: String,
    val materializedAppIds: List<AppId> = emptyList(),
    val canonicalConnectorId: String? = null,
    val category: String? = null,
    val description: String? = null,
    val logoUrl: String? = null,
    val logoUrlDark: String? = null,
    val reason: AppTemplateUnavailableReason? = null,
)

@Serializable
data class PluginHookSummary(
    val key: String,
    val eventName: HookEventName,
)

@Serializable
data class SkillSummary(
    val name: String,
    val description: String,
    val enabled: Boolean,
    @SerialName("interface")
    val interfaceMetadata: SkillInterface? = null,
    val path: CodexHostPath? = null,
    val shortDescription: String? = null,
)

@Serializable
data class SkillInterface(
    val brandColor: String? = null,
    val defaultPrompt: String? = null,
    val displayName: String? = null,
    val iconLarge: CodexHostPath? = null,
    val iconSmall: CodexHostPath? = null,
    val shortDescription: String? = null,
)
