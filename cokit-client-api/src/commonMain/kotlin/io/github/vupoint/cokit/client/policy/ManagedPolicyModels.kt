package io.github.vupoint.cokit.client.policy

import io.github.vupoint.cokit.client.CodexJsonPayload
import io.github.vupoint.cokit.client.ModelName
import io.github.vupoint.cokit.client.ReasoningEffort
import io.github.vupoint.cokit.client.ServiceTier
import io.github.vupoint.cokit.client.ApprovalPolicy
import io.github.vupoint.cokit.client.SandboxMode
import io.github.vupoint.cokit.client.environment.PermissionProfileId
import kotlinx.serialization.Serializable

@Serializable
data object ManagedPolicyReadParams

@Serializable
data class ManagedPolicyReadResult(
    val requirements: ManagedPolicyRequirements? = null,
)

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

@Serializable
@JvmInline
value class ManagedWebSearchMode(val value: String) {
    companion object {
        val Disabled = ManagedWebSearchMode("disabled")
        val Cached = ManagedWebSearchMode("cached")
        val Live = ManagedWebSearchMode("live")
    }
}

@Serializable
@JvmInline
value class ManagedWindowsSandboxSetupMode(val value: String) {
    companion object {
        val Elevated = ManagedWindowsSandboxSetupMode("elevated")
        val Unelevated = ManagedWindowsSandboxSetupMode("unelevated")
        val Mxc = ManagedWindowsSandboxSetupMode("mxc")
    }
}

@Serializable
@JvmInline
value class ManagedResidencyRequirement(val value: String) {
    companion object {
        val Us = ManagedResidencyRequirement("us")
    }
}

@Serializable
data class ManagedComputerUseRequirements(
    val allowLockedComputerUse: Boolean? = null,
)

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

@Serializable
@JvmInline
value class ManagedNetworkPermission(val value: String) {
    companion object {
        val Allow = ManagedNetworkPermission("allow")
        val Deny = ManagedNetworkPermission("deny")
    }
}

@Serializable
@JvmInline
value class ManagedLoginMethod(val value: String) {
    companion object {
        val ChatGpt = ManagedLoginMethod("chatgpt")
        val Api = ManagedLoginMethod("api")
    }
}

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

@Serializable
data class ManagedAutoReviewRequirements(val ignoreRules: List<String>? = null, val requiredOnModels: List<String>? = null)

@Serializable
data class ManagedInAppBrowserRequirements(val allowExternalBrowserSettingsImport: Boolean? = null)

@Serializable
data class ManagedFeedbackRequirements(val enabled: Boolean? = null)

@Serializable
data class ManagedModelsRequirements(val newThread: ManagedNewThreadModelDefaults? = null)

@Serializable
data class ManagedNewThreadModelDefaults(
    val model: ModelName? = null,
    val modelReasoningEffort: ReasoningEffort? = null,
    val serviceTier: ServiceTier? = null,
)

@Serializable
data class ManagedBrowserUseRequirements(
    val allowGlobalPersistentApproval: Boolean? = null,
    val allowHistoryAccess: Boolean? = null,
    val allowWebmcp: Boolean? = null,
    val defaultOriginPolicy: ManagedBrowserOriginPolicy? = null,
    val disableAutoReview: Boolean? = null,
    val origins: Map<String, ManagedBrowserOriginPolicy>? = null,
)

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

@Serializable
@JvmInline
value class ManagedRequirementDecision(val value: String) {
    companion object {
        val Allow = ManagedRequirementDecision("allow")
        val Deny = ManagedRequirementDecision("deny")
    }
}

@Serializable
@JvmInline
value class ManagedBrowserApprovalLifetime(val value: String) {
    companion object {
        val Turn = ManagedBrowserApprovalLifetime("turn")
        val Thread = ManagedBrowserApprovalLifetime("thread")
    }
}
