@file:OptIn(ExperimentalCodexApi::class)

package io.github.vupoint.cokit.client

import kotlinx.serialization.Serializable

/** Experimental project filesystem root on the app-server host. */
@ExperimentalCodexApi
@Serializable
data class ProjectRoot(
    val path: CodexHostPath,
)

/**
 * Experimental project metadata and ordered roots.
 *
 * Project APIs require experimental initialization and Kotlin API opt-in.
 *
 * @property id Opaque server project identifier.
 * @property metadata String-valued application metadata retained by the server.
 * @property position Server project ordering value.
 * @property createdAt Creation timestamp in Unix seconds.
 * @property updatedAt Update timestamp in Unix seconds.
 * @property recencyAt Optional recency timestamp in Unix seconds.
 */
@ExperimentalCodexApi
@Serializable
data class Project(
    val id: String,
    val name: String,
    val metadata: Map<String, String>,
    val roots: List<ProjectRoot>,
    val position: Long,
    val createdAt: CodexTimestamp,
    val updatedAt: CodexTimestamp,
    val recencyAt: CodexTimestamp? = null,
)

/** Filters and pagination for experimental project listing. Null options use server defaults. */
@ExperimentalCodexApi
@Serializable
data class ProjectListParams(
    val cursor: CodexCursor? = null,
    val limit: Int? = null,
    val sortDirection: SortDirection? = null,
    val sortKey: ProjectSortKey? = null,
)

/** Page of experimental projects with an optional opaque continuation token. */
@ExperimentalCodexApi
@Serializable
data class ProjectListResult(
    val data: List<Project>,
    val nextCursor: CodexCursor? = null,
)

/** Selects an experimental project by its server identifier. */
@ExperimentalCodexApi
@Serializable
data class ProjectReadParams(
    val projectId: String,
)

/** Experimental project returned by a read, create, import, or update operation. */
@ExperimentalCodexApi
@Serializable
data class ProjectResult(
    val project: Project,
)

/** Creates an experimental project using [idempotencyKey] to identify retries of the same operation. */
@ExperimentalCodexApi
@Serializable
data class ProjectCreateParams(
    val idempotencyKey: String,
    val name: String,
    val roots: List<ProjectRoot>,
    val metadata: Map<String, String>? = null,
)

/**
 * Imports an experimental project and optionally associates the supplied [threads].
 *
 * Reuse [idempotencyKey] only when retrying the same import.
 */
@ExperimentalCodexApi
@Serializable
data class ProjectImportParams(
    val idempotencyKey: String,
    val name: String,
    val roots: List<ProjectRoot>,
    val metadata: Map<String, String>? = null,
    val threads: List<ThreadId>? = null,
)

/** Updates the experimental project identified by [projectId]. Null changes are omitted. */
@ExperimentalCodexApi
@Serializable
data class ProjectUpdateParams(
    val projectId: String,
    val name: String? = null,
    val roots: List<ProjectRoot>? = null,
    val metadata: Map<String, String>? = null,
)

/**
 * Reorders an experimental project relative to [beforeProjectId].
 *
 * Null omits the explicit target position and uses the server's default placement.
 */
@ExperimentalCodexApi
@Serializable
data class ProjectMoveParams(
    val projectId: String,
    val beforeProjectId: String? = null,
)

/** Selects an experimental project for deletion by its server identifier. */
@ExperimentalCodexApi
@Serializable
data class ProjectDeleteParams(
    val projectId: String,
)

/** Experimental project list ordering key; unknown strings remain representable. */
@ExperimentalCodexApi
@Serializable
@JvmInline
value class ProjectSortKey(val value: String) {
    companion object {
        val Position = ProjectSortKey("position")
        val RecencyAt = ProjectSortKey("recencyAt")
    }
}

/** Experimental project change kind reported by [CodexNotification.ProjectChanged]. */
@ExperimentalCodexApi
@Serializable
@JvmInline
value class ProjectChangeType(val value: String) {
    companion object {
        val Created = ProjectChangeType("created")
        val Updated = ProjectChangeType("updated")
        val Deleted = ProjectChangeType("deleted")
    }
}
