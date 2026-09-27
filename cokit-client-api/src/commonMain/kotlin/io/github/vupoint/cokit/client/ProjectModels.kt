@file:OptIn(ExperimentalCodexApi::class)

package io.github.vupoint.cokit.client

import kotlinx.serialization.Serializable

@ExperimentalCodexApi
@Serializable
data class ProjectRoot(
    val path: CodexHostPath,
)

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

@ExperimentalCodexApi
@Serializable
data class ProjectListParams(
    val cursor: CodexCursor? = null,
    val limit: Int? = null,
    val sortDirection: SortDirection? = null,
    val sortKey: ProjectSortKey? = null,
)

@ExperimentalCodexApi
@Serializable
data class ProjectListResult(
    val data: List<Project>,
    val nextCursor: CodexCursor? = null,
)

@ExperimentalCodexApi
@Serializable
data class ProjectReadParams(
    val projectId: String,
)

@ExperimentalCodexApi
@Serializable
data class ProjectResult(
    val project: Project,
)

@ExperimentalCodexApi
@Serializable
data class ProjectCreateParams(
    val idempotencyKey: String,
    val name: String,
    val roots: List<ProjectRoot>,
    val metadata: Map<String, String>? = null,
)

@ExperimentalCodexApi
@Serializable
data class ProjectImportParams(
    val idempotencyKey: String,
    val name: String,
    val roots: List<ProjectRoot>,
    val metadata: Map<String, String>? = null,
    val threads: List<ThreadId>? = null,
)

@ExperimentalCodexApi
@Serializable
data class ProjectUpdateParams(
    val projectId: String,
    val name: String? = null,
    val roots: List<ProjectRoot>? = null,
    val metadata: Map<String, String>? = null,
)

@ExperimentalCodexApi
@Serializable
data class ProjectMoveParams(
    val projectId: String,
    val beforeProjectId: String? = null,
)

@ExperimentalCodexApi
@Serializable
data class ProjectDeleteParams(
    val projectId: String,
)

@ExperimentalCodexApi
@Serializable
@JvmInline
value class ProjectSortKey(val value: String) {
    companion object {
        val Position = ProjectSortKey("position")
        val RecencyAt = ProjectSortKey("recencyAt")
    }
}

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
