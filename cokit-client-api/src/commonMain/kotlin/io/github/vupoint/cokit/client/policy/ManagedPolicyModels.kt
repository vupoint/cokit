package io.github.vupoint.cokit.client.policy

import io.github.vupoint.cokit.client.CodexJsonPayload
import io.github.vupoint.cokit.client.ModelName
import io.github.vupoint.cokit.client.ReasoningEffort
import io.github.vupoint.cokit.client.ServiceTier
import io.github.vupoint.cokit.client.ApprovalPolicy
import io.github.vupoint.cokit.client.SandboxMode
import io.github.vupoint.cokit.client.environment.PermissionProfileId
import kotlinx.serialization.Serializable

/** Empty parameters for reading app-server managed configuration requirements. */
@Serializable
data object ManagedPolicyReadParams

/** Managed requirements snapshot; null [requirements] means no requirements payload was reported. */
@Serializable
data class ManagedPolicyReadResult(
    val requirements: ManagedPolicyRequirements? = null,
)

/**
 * Read-only constraints reported by app-server. CoKit preserves them without relaxing policy or granting access; null members are unspecified, not affirmative permissions.
 *
 * @property allowedPermissionProfiles Server-reported availability by profile id.
 * @property defaultPermissions Optional managed default profile.
 * @property featureRequirements Required feature states by server-defined key.
 * @property modelProviders Opaque managed provider configuration.
 * @property additionalDeveloperInstructions Managed instructions supplied by the server.
 */
@Serializable
data class ManagedPolicyRequirements(
    val allowedApprovalPolicies: List<ApprovalPolicy>? = null,
    val allowedSandboxModes: List<SandboxMode>? = null,
    val allowedWindowsSandboxImplementations: List<ManagedWindowsSandboxSetupMode>? = null,
    val allowedPermissionProfiles: Map<PermissionProfileId, Boolean>? = null,
    val defaultPermissions: PermissionProfileId? = null,
    val allowedWebSearchModes: List<ManagedWebSearchMode>? = null,
    val allowManagedHooksOnly: Boolean? = null,
    val allowAppshots: Boolean? = null,
    val allowRemoteControl: Boolean? = null,
    val computerUse: ManagedComputerUseRequirements? = null,
    val featureRequirements: Map<String, Boolean>? = null,
    val enforceResidency: ManagedResidencyRequirement? = null,
    @Deprecated("Not present in the 0.157.1 stable requirements schema; retained for older servers")
    val network: ManagedNetworkRequirements? = null,
    val allowedLoginMethods: List<ManagedLoginMethod>? = null,
    val modelProvider: String? = null,
    val modelProviders: Map<String, CodexJsonPayload>? = null,
    val additionalDeveloperInstructions: String? = null,
    val allowBrowserAndComputerUse: Boolean? = null,
    val allowLoginShell: Boolean? = null,
    val autoReview: ManagedAutoReviewRequirements? = null,
    val browserUse: ManagedBrowserUseRequirements? = null,
    val inAppBrowser: ManagedInAppBrowserRequirements? = null,
    val chatgptBaseUrl: String? = null,
    val cliAuthCredentialsStore: ManagedCredentialsStore? = null,
    val checkForUpdateOnStartup: Boolean? = null,
    val feedback: ManagedFeedbackRequirements? = null,
    val logDir: String? = null,
    val modelCatalogJson: String? = null,
    val sqliteHome: String? = null,
    val models: ManagedModelsRequirements? = null,
)

/** Managed web-search mode selector; unknown wire strings are retained. */
@Serializable
@JvmInline
value class ManagedWebSearchMode(val value: String) {
    companion object {
        val Disabled = ManagedWebSearchMode("disabled")
        val Cached = ManagedWebSearchMode("cached")
        val Live = ManagedWebSearchMode("live")
    }
}

/** Managed Windows sandbox implementation selector; unknown wire strings are retained. */
@Serializable
@JvmInline
value class ManagedWindowsSandboxSetupMode(val value: String) {
    companion object {
        val Elevated = ManagedWindowsSandboxSetupMode("elevated")
        val Unelevated = ManagedWindowsSandboxSetupMode("unelevated")
        val Mxc = ManagedWindowsSandboxSetupMode("mxc")
    }
}

/** Managed data residency requirement reported by the server; unknown strings are retained. */
@Serializable
@JvmInline
value class ManagedResidencyRequirement(val value: String) {
    companion object {
        val Us = ManagedResidencyRequirement("us")
    }
}

/** Managed restriction on computer use while the host is locked; null leaves the requirement unspecified. */
@Serializable
data class ManagedComputerUseRequirements(
    val allowLockedComputerUse: Boolean? = null,
)

/**
 * Legacy managed network constraints retained for older servers. Null fields are unspecified and do not grant network or socket access.
 */
@Serializable
data class ManagedNetworkRequirements(
    val enabled: Boolean? = null,
    val httpPort: Int? = null,
    val socksPort: Int? = null,
    val allowUpstreamProxy: Boolean? = null,
    val dangerouslyAllowNonLoopbackProxy: Boolean? = null,
    val dangerouslyAllowAllUnixSockets: Boolean? = null,
    val domains: Map<String, ManagedNetworkPermission>? = null,
    val managedAllowedDomainsOnly: Boolean? = null,
    val unixSockets: Map<String, ManagedNetworkPermission>? = null,
    val allowLocalBinding: Boolean? = null,
)

/** Allow or deny rule in legacy managed network requirements; unknown strings are retained. */
@Serializable
@JvmInline
value class ManagedNetworkPermission(val value: String) {
    companion object {
        val Allow = ManagedNetworkPermission("allow")
        val Deny = ManagedNetworkPermission("deny")
    }
}

/** Authentication methods permitted by managed policy; unknown strings are retained. */
@Serializable
@JvmInline
value class ManagedLoginMethod(val value: String) {
    companion object {
        val ChatGpt = ManagedLoginMethod("chatgpt")
        val Api = ManagedLoginMethod("api")
    }
}

/** Managed credential storage selection, including ephemeral storage; CoKit does not implement or change the store itself. */
@Serializable
@JvmInline
value class ManagedCredentialsStore(val value: String) {
    companion object {
        val File = ManagedCredentialsStore("file")
        val Keyring = ManagedCredentialsStore("keyring")
        val Auto = ManagedCredentialsStore("auto")
        val Ephemeral = ManagedCredentialsStore("ephemeral")
    }
}

/** Managed automatic-review rules and model restrictions; null lists leave those requirements unspecified. */
@Serializable
data class ManagedAutoReviewRequirements(val ignoreRules: List<String>? = null, val requiredOnModels: List<String>? = null)

/** Managed permission for importing external browser settings; an unspecified value is not a local authorization. */
@Serializable
data class ManagedInAppBrowserRequirements(val allowExternalBrowserSettingsImport: Boolean? = null)

/** Managed feedback enablement; null indicates no reported requirement. */
@Serializable
data class ManagedFeedbackRequirements(val enabled: Boolean? = null)

/** Optional managed model defaults applied to new threads by app-server. */
@Serializable
data class ManagedModelsRequirements(val newThread: ManagedNewThreadModelDefaults? = null)

/** Managed model, reasoning effort, and service-tier defaults for newly created threads. */
@Serializable
data class ManagedNewThreadModelDefaults(
    val model: ModelName? = null,
    val modelReasoningEffort: ReasoningEffort? = null,
    val serviceTier: ServiceTier? = null,
)

/** Managed browser access and approval restrictions. CoKit exposes these as policy state rather than browser capabilities. */
@Serializable
data class ManagedBrowserUseRequirements(
    val allowGlobalPersistentApproval: Boolean? = null,
    val allowHistoryAccess: Boolean? = null,
    val allowWebmcp: Boolean? = null,
    val defaultOriginPolicy: ManagedBrowserOriginPolicy? = null,
    val disableAutoReview: Boolean? = null,
    val origins: Map<String, ManagedBrowserOriginPolicy>? = null,
)

/** Managed access, upload, download, and approval rules for a browser origin; null members are unspecified rather than allowed. */
@Serializable
data class ManagedBrowserOriginPolicy(
    val access: ManagedRequirementDecision? = null,
    val accessApprovalLifetime: ManagedBrowserApprovalLifetime? = null,
    val autoReview: ManagedRequirementDecision? = null,
    val downloads: ManagedRequirementDecision? = null,
    val fullCdpAccess: ManagedRequirementDecision? = null,
    val persistentApproval: Boolean? = null,
    val uploads: ManagedRequirementDecision? = null,
)

/** Explicit allow or deny decision in managed browser requirements; unknown strings are retained. */
@Serializable
@JvmInline
value class ManagedRequirementDecision(val value: String) {
    companion object {
        val Allow = ManagedRequirementDecision("allow")
        val Deny = ManagedRequirementDecision("deny")
    }
}

/** Managed browser approval lifetime, scoped to a turn or thread; unknown strings are retained. */
@Serializable
@JvmInline
value class ManagedBrowserApprovalLifetime(val value: String) {
    companion object {
        val Turn = ManagedBrowserApprovalLifetime("turn")
        val Thread = ManagedBrowserApprovalLifetime("thread")
    }
}
