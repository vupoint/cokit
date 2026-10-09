package io.github.vupoint.cokit.client.plugins

import io.github.vupoint.cokit.client.CodexHostPath
import io.github.vupoint.cokit.client.extensions.AppId
import io.github.vupoint.cokit.client.extensions.HookEventName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Marketplace source category used to filter plugin discovery; unknown wire strings are retained. */
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

/** Whether plugin authentication is required at installation or use time, as reported by app-server. */
@Serializable
@JvmInline
value class PluginAuthPolicy(val value: String) {
    companion object {
        val OnInstall = PluginAuthPolicy("ON_INSTALL")
        val OnUse = PluginAuthPolicy("ON_USE")
    }
}

/** Server-reported installation availability or default-install policy; it does not itself install a plugin. */
@Serializable
@JvmInline
value class PluginInstallPolicy(val value: String) {
    companion object {
        val NotAvailable = PluginInstallPolicy("NOT_AVAILABLE")
        val Available = PluginInstallPolicy("AVAILABLE")
        val InstalledByDefault = PluginInstallPolicy("INSTALLED_BY_DEFAULT")
    }
}

/** Administrative plugin availability reported by the catalog; unknown wire strings are retained. */
@Serializable
@JvmInline
value class PluginAvailability(val value: String) {
    companion object {
        val DisabledByAdmin = PluginAvailability("DISABLED_BY_ADMIN")
        val Available = PluginAvailability("AVAILABLE")
    }
}

/** Visibility policy reported for a shared plugin, including listed, unlisted, and private. */
@Serializable
@JvmInline
value class PluginShareDiscoverability(val value: String) {
    companion object {
        val Listed = PluginShareDiscoverability("LISTED")
        val Unlisted = PluginShareDiscoverability("UNLISTED")
        val Private = PluginShareDiscoverability("PRIVATE")
    }
}

/** Kind of account principal in a plugin sharing record; unknown strings are retained. */
@Serializable
@JvmInline
value class PluginSharePrincipalType(val value: String) {
    companion object {
        val User = PluginSharePrincipalType("user")
        val Group = PluginSharePrincipalType("group")
        val Workspace = PluginSharePrincipalType("workspace")
    }
}

/** Access role reported for a plugin sharing principal; this metadata does not grant permissions locally. */
@Serializable
@JvmInline
value class PluginSharePrincipalRole(val value: String) {
    companion object {
        val Reader = PluginSharePrincipalRole("reader")
        val Editor = PluginSharePrincipalRole("editor")
        val Owner = PluginSharePrincipalRole("owner")
    }
}

/** Server explanation of why a plugin app template cannot be materialized; unknown strings are retained. */
@Serializable
@JvmInline
value class AppTemplateUnavailableReason(val value: String) {
    companion object {
        val NotConfiguredForWorkspace = AppTemplateUnavailableReason("NOT_CONFIGURED_FOR_WORKSPACE")
        val NoActiveWorkspace = AppTemplateUnavailableReason("NO_ACTIVE_WORKSPACE")
    }
}

/** Discovers plugins for optional host directories and marketplace categories; null filters leave server selection in effect. */
@Serializable
data class PluginListParams(
    val cwds: List<CodexHostPath>? = null,
    val marketplaceKinds: List<PluginListMarketplaceKind>? = null,
)

/** Lists installed plugins for optional host directories, with optional plugin names used for install suggestions. */
@Serializable
data class PluginInstalledParams(
    val cwds: List<CodexHostPath>? = null,
    val installSuggestionPluginNames: List<String>? = null,
)

/** Selects a plugin by name and optional local or remote marketplace context for reading details. */
@Serializable
data class PluginReadParams(
    val pluginName: String,
    val marketplacePath: CodexHostPath? = null,
    val remoteMarketplaceName: String? = null,
)

/**
 * Explicit plugin installation on the app-server host. Applications must evaluate the selected source and returned authentication requirements.
 */
@Serializable
data class PluginInstallParams(
    val pluginName: String,
    val marketplacePath: CodexHostPath? = null,
    val remoteMarketplaceName: String? = null,
)

/** Explicit removal of the installed plugin selected by its server-issued id. */
@Serializable
data class PluginUninstallParams(
    val pluginId: String,
)

/** Reads a named skill from a remote marketplace plugin; the resulting contents are untrusted instructions. */
@Serializable
data class PluginSkillReadParams(
    val remoteMarketplaceName: String,
    val remotePluginId: String,
    val skillName: String,
)

/**
 * Adds a marketplace source to app-server's local plugin state. Applications must authorize the source before invoking the mutation.
 *
 * @property refName Optional repository ref selection.
 * @property sparsePaths Optional repository paths to include in a sparse checkout.
 */
@Serializable
data class MarketplaceAddParams(
    val source: String,
    val refName: String? = null,
    val sparsePaths: List<String>? = null,
)

/** Explicit removal of the marketplace selected by its configured name. */
@Serializable
data class MarketplaceRemoveParams(
    val marketplaceName: String,
)

/** Requests marketplace updates; null [marketplaceName] delegates marketplace selection to app-server. */
@Serializable
data class MarketplaceUpgradeParams(
    val marketplaceName: String? = null,
)

/** Plugin discovery grouped by marketplace, including featured ids and per-marketplace load failures. */
@Serializable
data class PluginListResult(
    val marketplaces: List<PluginMarketplaceEntry> = emptyList(),
    val featuredPluginIds: List<String> = emptyList(),
    val marketplaceLoadErrors: List<MarketplaceLoadErrorInfo> = emptyList(),
)

/** Installed plugin inventory grouped by marketplace with any load failures. */
@Serializable
data class PluginInstalledResult(
    val marketplaces: List<PluginMarketplaceEntry> = emptyList(),
    val marketplaceLoadErrors: List<MarketplaceLoadErrorInfo> = emptyList(),
)

/** Full server-reported details of the selected plugin, without loading or executing plugin code in CoKit. */
@Serializable
data class PluginReadResult(
    val plugin: PluginDetail,
)

/** Optional skill file contents from a remote plugin; null means no contents were returned, not an empty file. */
@Serializable
data class PluginSkillReadResult(
    val contents: String? = null,
)

/**
 * Installation authentication policy and apps still requiring authentication; installation does not imply those apps are authenticated.
 */
@Serializable
data class PluginInstallResult(
    val authPolicy: PluginAuthPolicy,
    val appsNeedingAuth: List<AppSummary> = emptyList(),
)

/** Added or existing marketplace identity and its installed root on the app-server host. */
@Serializable
data class MarketplaceAddResult(
    val alreadyAdded: Boolean,
    val installedRoot: CodexHostPath,
    val marketplaceName: String,
)

/** Removed marketplace name and optional former installation root reported by app-server. */
@Serializable
data class MarketplaceRemoveResult(
    val marketplaceName: String,
    val installedRoot: CodexHostPath? = null,
)

/**
 * Marketplace update summary with selected names, updated host roots, and individual failures; a successful RPC can still contain errors.
 */
@Serializable
data class MarketplaceUpgradeResult(
    val errors: List<MarketplaceUpgradeErrorInfo> = emptyList(),
    val selectedMarketplaces: List<String> = emptyList(),
    val upgradedRoots: List<CodexHostPath> = emptyList(),
)

/** Update failure for one marketplace, preserved with its server diagnostic message. */
@Serializable
data class MarketplaceUpgradeErrorInfo(
    val marketplaceName: String,
    val message: String,
)

/** Discovery failure for a marketplace at an app-server host path. */
@Serializable
data class MarketplaceLoadErrorInfo(
    val marketplacePath: CodexHostPath,
    val message: String,
)

/** Optional marketplace display metadata; CoKit does not render a marketplace interface. */
@Serializable
data class MarketplaceInterface(
    val displayName: String? = null,
)

/** Marketplace identity, optional host location, and plugin summaries discovered within it. */
@Serializable
data class PluginMarketplaceEntry(
    val name: String,
    val plugins: List<PluginSummary> = emptyList(),
    @SerialName("interface")
    val interfaceMetadata: MarketplaceInterface? = null,
    val path: CodexHostPath? = null,
)

/**
 * Plugin identity, source, installation state, availability, and sharing metadata. Catalog discovery does not execute plugin code.
 *
 * @property installed Whether app-server reports the plugin installed.
 * @property enabled Whether the installed plugin is enabled.
 * @property localVersion Optional version of the local installation.
 * @property remotePluginId Optional remote catalog identity, distinct from the installed plugin id.
 */
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

/** Plugin origin reported by app-server; local paths and Git URLs are untrusted source metadata, not permission to load code. */
@Serializable
sealed interface PluginSource {
    /** Plugin source located at a path on the app-server host. */
    @Serializable
    @SerialName("local")
    data class Local(
        val path: CodexHostPath,
    ) : PluginSource

    /** Plugin Git origin with optional repository subpath, ref, and revision; CoKit does not clone it while reading metadata. */
    @Serializable
    @SerialName("git")
    data class Git(
        val url: String,
        val path: String? = null,
        val refName: String? = null,
        val sha: String? = null,
    ) : PluginSource

    /** Plugin obtained from a remote marketplace rather than a reported local or Git source. */
    @Serializable
    @SerialName("remote")
    data object Remote : PluginSource
}

/** Plugin presentation metadata with host asset paths or remote URLs; CoKit does not fetch assets or render UI. */
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

/** Remote plugin sharing metadata. Principal ids, creator data, and share URLs may contain private account information. */
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

/** Account, group, or workspace sharing principal and its reported plugin access role. */
@Serializable
data class PluginSharePrincipal(
    val name: String,
    val principalId: String,
    val principalType: PluginSharePrincipalType,
    val role: PluginSharePrincipalRole,
)

/** Expanded plugin inventory of apps, templates, hooks, MCP server names, and skills; reading details does not activate them. */
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

/** App integration advertised by a plugin, with an optional installation URL for application-mediated setup. */
@Serializable
data class AppSummary(
    val id: AppId,
    val name: String,
    val category: String? = null,
    val description: String? = null,
    val installUrl: String? = null,
)

/**
 * Plugin app template and any materialized connector ids.
 *
 * @property reason Optional explanation of template unavailability.
 * @property materializedAppIds App ids already produced from this template.
 */
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

/** Hook key and trigger advertised by a plugin; no hook execution occurs when reading this summary. */
@Serializable
data class PluginHookSummary(
    val key: String,
    val eventName: HookEventName,
)

/** Skill metadata advertised by a plugin, including enablement and an optional host source path. */
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

/** Plugin skill display metadata; host icon paths are not client-local paths and are not rendered by CoKit. */
@Serializable
data class SkillInterface(
    val brandColor: String? = null,
    val defaultPrompt: String? = null,
    val displayName: String? = null,
    val iconLarge: CodexHostPath? = null,
    val iconSmall: CodexHostPath? = null,
    val shortDescription: String? = null,
)
