package io.github.vupoint.cokit.client.skills

import io.github.vupoint.cokit.client.CodexHostPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Server-reported skill selector used in catalogs and skill configuration changes. */
@Serializable
@JvmInline
value class SkillName(val value: String)

/** Skill discovery source scope, such as repository, user, system, or admin; unknown strings are retained. */
@Serializable
@JvmInline
value class SkillScope(val value: String) {
    companion object {
        val User = SkillScope("user")
        val Repo = SkillScope("repo")
        val System = SkillScope("system")
        val Admin = SkillScope("admin")
    }
}

/** Declared skill dependency kind, such as MCP or command; catalog discovery does not invoke the dependency. */
@Serializable
@JvmInline
value class SkillToolDependencyType(val value: String) {
    companion object {
        val Mcp = SkillToolDependencyType("mcp")
        val Command = SkillToolDependencyType("command")
    }
}

/** Lists skill metadata for host working directories, optionally forcing server-side reload. */
@Serializable
data class SkillsListParams(
    val cwds: List<CodexHostPath> = emptyList(),
    val forceReload: Boolean? = null,
)

/** Skill discovery grouped by app-server host working directory. */
@Serializable
data class SkillsListResult(
    val data: List<SkillsListEntry> = emptyList(),
)

/** Discovered skills and parse errors for one host working directory. */
@Serializable
data class SkillsListEntry(
    val cwd: CodexHostPath,
    val errors: List<SkillErrorInfo> = emptyList(),
    val skills: List<SkillMetadata> = emptyList(),
)

/** Skill discovery failure with a server-reported source path and diagnostic message. */
@Serializable
data class SkillErrorInfo(
    val message: String,
    val path: CodexHostPath,
)

/**
 * Skill source, scope, enablement, and dependency metadata. CoKit does not execute skill instructions or dependencies when reading this record.
 */
@Serializable
data class SkillMetadata(
    val name: SkillName,
    val description: String,
    val enabled: Boolean,
    val path: CodexHostPath,
    val scope: SkillScope,
    val shortDescription: String? = null,
    @SerialName("interface")
    val interfaceMetadata: SkillInterfaceMetadata? = null,
    val dependencies: SkillDependencies? = null,
)

/** Skill presentation metadata with optional host icon paths; CoKit does not fetch assets or render UI. */
@Serializable
data class SkillInterfaceMetadata(
    val brandColor: String? = null,
    val defaultPrompt: String? = null,
    val displayName: String? = null,
    val iconLarge: CodexHostPath? = null,
    val iconSmall: CodexHostPath? = null,
    val shortDescription: String? = null,
)

/** Tool dependencies declared by a skill; discovery does not install or authorize them. */
@Serializable
data class SkillDependencies(
    val tools: List<SkillToolDependency> = emptyList(),
)

/**
 * Declared tool dependency metadata. Commands, transports, and URLs are untrusted declarations rather than instructions CoKit executes.
 */
@Serializable
data class SkillToolDependency(
    val type: SkillToolDependencyType,
    val value: String,
    val command: String? = null,
    val description: String? = null,
    val transport: String? = null,
    val url: String? = null,
)

/** Changes additional host directories searched for skills; applications must authorize discovery outside the active project. */
@Serializable
data class SkillsExtraRootsSetParams(
    val extraRoots: List<CodexHostPath>,
)

/** Explicit skill enablement edit selected by optional name or host path; selector validation belongs to app-server. */
@Serializable
data class SkillConfigWriteParams(
    val enabled: Boolean,
    val name: SkillName? = null,
    val path: CodexHostPath? = null,
)

/** Effective skill enablement reported after a configuration write. */
@Serializable
data class SkillConfigWriteResult(
    val effectiveEnabled: Boolean,
)
