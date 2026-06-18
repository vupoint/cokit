package io.github.cokit.client.skills

import io.github.cokit.client.CodexHostPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class SkillName(val value: String)

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

@Serializable
@JvmInline
value class SkillToolDependencyType(val value: String) {
    companion object {
        val Mcp = SkillToolDependencyType("mcp")
        val Command = SkillToolDependencyType("command")
    }
}

@Serializable
data class SkillsListParams(
    val cwds: List<CodexHostPath> = emptyList(),
    val forceReload: Boolean? = null,
)

@Serializable
data class SkillsListResult(
    val data: List<SkillsListEntry> = emptyList(),
)

@Serializable
data class SkillsListEntry(
    val cwd: CodexHostPath,
    val errors: List<SkillErrorInfo> = emptyList(),
    val skills: List<SkillMetadata> = emptyList(),
)

@Serializable
data class SkillErrorInfo(
    val message: String,
    val path: CodexHostPath,
)

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

@Serializable
data class SkillInterfaceMetadata(
    val brandColor: String? = null,
    val defaultPrompt: String? = null,
    val displayName: String? = null,
    val iconLarge: CodexHostPath? = null,
    val iconSmall: CodexHostPath? = null,
    val shortDescription: String? = null,
)

@Serializable
data class SkillDependencies(
    val tools: List<SkillToolDependency> = emptyList(),
)

@Serializable
data class SkillToolDependency(
    val type: SkillToolDependencyType,
    val value: String,
    val command: String? = null,
    val description: String? = null,
    val transport: String? = null,
    val url: String? = null,
)

@Serializable
data class SkillsExtraRootsSetParams(
    val extraRoots: List<CodexHostPath>,
)

@Serializable
data class SkillConfigWriteParams(
    val enabled: Boolean,
    val name: SkillName? = null,
    val path: CodexHostPath? = null,
)

@Serializable
data class SkillConfigWriteResult(
    val effectiveEnabled: Boolean,
)
