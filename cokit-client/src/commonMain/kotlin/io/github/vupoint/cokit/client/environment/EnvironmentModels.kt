package io.github.vupoint.cokit.client.environment

import io.github.vupoint.cokit.client.CodexCursor
import io.github.vupoint.cokit.client.CodexHostPath
import io.github.vupoint.cokit.client.ModelName
import io.github.vupoint.cokit.client.ReasoningEffort
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class PermissionProfileId(val value: String)

@Serializable
@JvmInline
value class EnvironmentId(val value: String)

@Serializable
@JvmInline
value class CollaborationModeKind(val value: String) {
    companion object {
        val Plan = CollaborationModeKind("plan")
        val Default = CollaborationModeKind("default")
    }
}

@Serializable
data class PermissionProfileListParams(
    val cursor: CodexCursor? = null,
    val cwd: CodexHostPath? = null,
    val limit: Int? = null,
)

@Serializable
data class PermissionProfileListResult(
    val data: List<PermissionProfileSummary> = emptyList(),
    val nextCursor: CodexCursor? = null,
)

@Serializable
data class PermissionProfileSummary(
    val id: PermissionProfileId,
    val description: String? = null,
)

@Serializable
data object CollaborationModeListParams

@Serializable
data class CollaborationModeListResult(
    val data: List<CollaborationModePreset> = emptyList(),
)

@Serializable
data class CollaborationModePreset(
    val name: String,
    val mode: CollaborationModeKind? = null,
    val model: ModelName? = null,
    @SerialName("reasoning_effort")
    val reasoningEffort: ReasoningEffort? = null,
)

@Serializable
data class EnvironmentAddParams(
    val environmentId: EnvironmentId,
    val execServerUrl: String,
)
