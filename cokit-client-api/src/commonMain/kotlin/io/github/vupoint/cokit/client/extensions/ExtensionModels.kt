package io.github.vupoint.cokit.client.extensions

import io.github.vupoint.cokit.client.CodexCursor
import io.github.vupoint.cokit.client.CodexHostPath
import io.github.vupoint.cokit.client.ThreadId
import kotlinx.serialization.Serializable

/** Server hook trigger name; unknown event names remain readable as catalog metadata. */
@Serializable
@JvmInline
value class HookEventName(val value: String) {
    companion object {
        val PreToolUse = HookEventName("preToolUse")
        val PermissionRequest = HookEventName("permissionRequest")
        val PostToolUse = HookEventName("postToolUse")
        val PreCompact = HookEventName("preCompact")
        val PostCompact = HookEventName("postCompact")
        val SessionStart = HookEventName("sessionStart")
        val UserPromptSubmit = HookEventName("userPromptSubmit")
        val SubagentStart = HookEventName("subagentStart")
        val SubagentStop = HookEventName("subagentStop")
        val Stop = HookEventName("stop")
    }
}

/** Configured hook implementation kind; listing it does not execute a command, prompt, or agent. */
@Serializable
@JvmInline
value class HookHandlerType(val value: String) {
    companion object {
        val Command = HookHandlerType("command")
        val Prompt = HookHandlerType("prompt")
        val Agent = HookHandlerType("agent")
    }
}

/** Configuration source that supplied a hook; unknown sources remain readable. */
@Serializable
@JvmInline
value class HookSource(val value: String) {
    companion object {
        val System = HookSource("system")
        val User = HookSource("user")
        val Project = HookSource("project")
        val Mdm = HookSource("mdm")
        val SessionFlags = HookSource("sessionFlags")
        val Plugin = HookSource("plugin")
        val CloudRequirements = HookSource("cloudRequirements")
        val CloudManagedConfig = HookSource("cloudManagedConfig")
        val LegacyManagedConfigFile = HookSource("legacyManagedConfigFile")
        val LegacyManagedConfigMdm = HookSource("legacyManagedConfigMdm")
        val Unknown = HookSource("unknown")
    }
}

/** Server-reported hook trust state; CoKit does not grant trust from this value. */
@Serializable
@JvmInline
value class HookTrustStatus(val value: String) {
    companion object {
        val Managed = HookTrustStatus("managed")
        val Untrusted = HookTrustStatus("untrusted")
        val Trusted = HookTrustStatus("trusted")
        val Modified = HookTrustStatus("modified")
    }
}

/** Lists hook metadata for app-server host working directories; CoKit does not execute the returned hooks. */
@Serializable
data class HooksListParams(
    val cwds: List<CodexHostPath> = emptyList(),
)

/** Hook discovery grouped by the working directories evaluated by app-server. */
@Serializable
data class HooksListResult(
    val data: List<HooksListEntry> = emptyList(),
)

/** Hooks, parse errors, and warnings for one app-server host directory. */
@Serializable
data class HooksListEntry(
    val cwd: CodexHostPath,
    val errors: List<HookErrorInfo> = emptyList(),
    val hooks: List<HookMetadata> = emptyList(),
    val warnings: List<String> = emptyList(),
)

/** Hook discovery failure with the server-reported source path and diagnostic message. */
@Serializable
data class HookErrorInfo(
    val message: String,
    val path: CodexHostPath,
)

/**
 * Server-reported hook configuration and trust metadata, treated as catalog data rather than executable instructions.
 *
 * @property timeoutSec Configured hook timeout in seconds.
 * @property currentHash Server-reported current content hash.
 * @property command Optional configured command; CoKit does not execute it while listing hooks.
 * @property isManaged Whether app-server reports the hook as managed configuration.
 */
@Serializable
data class HookMetadata(
    val key: String,
    val eventName: HookEventName,
    val handlerType: HookHandlerType,
    val source: HookSource,
    val sourcePath: CodexHostPath,
    val enabled: Boolean,
    val timeoutSec: Long,
    val trustStatus: HookTrustStatus,
    val currentHash: String,
    val displayOrder: Long,
    val isManaged: Boolean,
    val command: String? = null,
    val matcher: String? = null,
    val pluginId: String? = null,
    val statusMessage: String? = null,
)

/** Server app or connector identifier used by catalogs and plugin app summaries. */
@Serializable
@JvmInline
value class AppId(val value: String)

/** Reads installed app state, optionally refreshing it and selecting a thread context. */
@Serializable
data class AppsInstalledParams(
    val forceRefresh: Boolean = false,
    val threadId: ThreadId? = null,
)

/** Installed app metadata reported by app-server; enablement and callability are separate properties. */
@Serializable
data class AppsInstalledResult(
    val apps: List<InstalledApp>,
)

/**
 * Installed connector state.
 *
 * @property enabled Whether the app is enabled.
 * @property callable Whether app-server currently reports it as callable.
 */
@Serializable
data class InstalledApp(
    val id: AppId,
    val enabled: Boolean,
    val callable: Boolean,
    val runtimeName: String? = null,
)

/** Queries the app catalog for an optional thread context; invocation of the experimental descriptor requires explicit opt-in. */
@Serializable
data class AppsListParams(
    val cursor: CodexCursor? = null,
    val forceRefetch: Boolean? = null,
    val limit: Int? = null,
    val threadId: ThreadId? = null,
)

/** One page of app catalog entries; null [nextCursor] means no continuation was reported. */
@Serializable
data class AppsListResult(
    val data: List<AppInfo> = emptyList(),
    val nextCursor: CodexCursor? = null,
)

/**
 * App catalog and presentation metadata; CoKit does not install, authenticate, render, or invoke an app from this record.
 *
 * @property isAccessible Server-reported account access, separate from enablement.
 * @property isEnabled Server-reported app enablement.
 */
@Serializable
data class AppInfo(
    val id: AppId,
    val name: String,
    val appMetadata: AppMetadata? = null,
    val branding: AppBranding? = null,
    val description: String? = null,
    val distributionChannel: String? = null,
    val installUrl: String? = null,
    val isAccessible: Boolean = false,
    val isEnabled: Boolean = true,
    val labels: Map<String, String>? = null,
    val logoUrl: String? = null,
    val logoUrlDark: String? = null,
    val pluginDisplayNames: List<String> = emptyList(),
)

/** App discovery and publisher presentation metadata supplied by the catalog. */
@Serializable
data class AppBranding(
    val isDiscoverableApp: Boolean,
    val category: String? = null,
    val developer: String? = null,
    val privacyPolicy: String? = null,
    val termsOfService: String? = null,
    val website: String? = null,
)

/** Optional app catalog details for presentation, installation hints, and version information. */
@Serializable
data class AppMetadata(
    val categories: List<String>? = null,
    val developer: String? = null,
    val firstPartyRequiresInstall: Boolean? = null,
    val firstPartyType: String? = null,
    val review: AppReview? = null,
    val screenshots: List<AppScreenshot>? = null,
    val seoDescription: String? = null,
    val showInComposerWhenUnlinked: Boolean? = null,
    val subCategories: List<String>? = null,
    val version: String? = null,
    val versionId: String? = null,
    val versionNotes: String? = null,
)

/** Review status reported by the app catalog; CoKit does not interpret it as a trust decision. */
@Serializable
data class AppReview(
    val status: String,
)

/** Catalog screenshot reference and its illustrative prompt; no media is fetched or rendered by this model. */
@Serializable
data class AppScreenshot(
    val userPrompt: String,
    val fileId: String? = null,
    val url: String? = null,
)
