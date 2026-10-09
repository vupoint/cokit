package io.github.vupoint.cokit.client.environment

import io.github.vupoint.cokit.client.CodexCursor
import io.github.vupoint.cokit.client.CodexHostPath
import io.github.vupoint.cokit.client.ModelName
import io.github.vupoint.cokit.client.ReasoningEffort
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Server-defined permission profile selector, used in catalog entries and managed policy constraints. */
@Serializable
@JvmInline
value class PermissionProfileId(val value: String)

/** App-server execution environment identifier, distinct from thread and process identifiers. */
@Serializable
@JvmInline
value class EnvironmentId(val value: String)

/** Server collaboration behavior, such as plan or default mode; unknown strings are retained. */
@Serializable
@JvmInline
value class CollaborationModeKind(val value: String) {
    companion object {
        val Plan = CollaborationModeKind("plan")
        val Default = CollaborationModeKind("default")
    }
}

/** Queries permission profiles available for an optional host working directory; null paging options use server defaults. */
@Serializable
data class PermissionProfileListParams(
    val cursor: CodexCursor? = null,
    val cwd: CodexHostPath? = null,
    val limit: Int? = null,
)

/** One page of permission profiles; null [nextCursor] means no continuation was reported. */
@Serializable
data class PermissionProfileListResult(
    val data: List<PermissionProfileSummary> = emptyList(),
    val nextCursor: CodexCursor? = null,
)

/** Catalog metadata for a named permission profile. Availability is server-reported and does not itself grant the profile. */
@Serializable
data class PermissionProfileSummary(
    val id: PermissionProfileId,
    /** Null means an older server did not report availability. */
    val allowed: Boolean? = null,
    val description: String? = null,
)

/** Empty parameters for reading available collaboration mode presets. */
@Serializable
data object CollaborationModeListParams

/** Server-defined collaboration presets suitable for selecting a mode and its defaults. */
@Serializable
data class CollaborationModeListResult(
    val data: List<CollaborationModePreset> = emptyList(),
)

/** Named collaboration preset with optional mode, model, and reasoning defaults; absent values are not inferred by CoKit. */
@Serializable
data class CollaborationModePreset(
    val name: String,
    val mode: CollaborationModeKind? = null,
    val model: ModelName? = null,
    @SerialName("reasoning_effort")
    val reasoningEffort: ReasoningEffort? = null,
)

/**
 * Registers a host execution environment with app-server. The endpoint is a trust boundary; registering it can direct execution to another host.
 *
 * @property execServerUrl Execution server endpoint, not a URL CoKit opens or verifies locally.
 */
@Serializable
data class EnvironmentAddParams(
    val environmentId: EnvironmentId,
    val execServerUrl: String,
)
