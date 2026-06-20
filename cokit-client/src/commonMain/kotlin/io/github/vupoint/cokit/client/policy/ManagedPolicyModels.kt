package io.github.vupoint.cokit.client.policy

import io.github.vupoint.cokit.client.ApprovalPolicy
import io.github.vupoint.cokit.client.SandboxPolicy
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
    val allowedSandboxModes: List<SandboxPolicy>? = null,
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
    val network: ManagedNetworkRequirements? = null,
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
