package io.github.vupoint.cokit.client.approvals

import io.github.vupoint.cokit.client.CodexHostPath
import io.github.vupoint.cokit.client.ItemId
import io.github.vupoint.cokit.client.ThreadId
import io.github.vupoint.cokit.client.TurnId
import kotlinx.serialization.Serializable

@Serializable
data class PermissionApprovalRequest(
    val threadId: ThreadId,
    val turnId: TurnId,
    val itemId: ItemId,
    val startedAtMs: Long,
    val environmentId: PermissionEnvironmentId? = null,
    val cwd: CodexHostPath,
    val reason: String? = null,
    val permissions: PermissionProfile,
)

@Serializable
@JvmInline
value class PermissionEnvironmentId(val value: String)

@Serializable
data class PermissionProfile(
    val fileSystem: PermissionFileSystem? = null,
    val network: PermissionNetwork? = null,
)

@Serializable
data class PermissionFileSystem(
    val entries: List<FileSystemPermissionEntry>? = null,
    val globScanMaxDepth: Int? = null,
    val read: List<CodexHostPath>? = null,
    val write: List<CodexHostPath>? = null,
)

@Serializable
data class FileSystemPermissionEntry(
    val access: FileSystemPermissionAccess,
    val path: FileSystemPermissionPath,
)

@Serializable
@JvmInline
value class FileSystemPermissionAccess(val value: String) {
    companion object {
        val Read = FileSystemPermissionAccess("read")
        val Write = FileSystemPermissionAccess("write")
        val Deny = FileSystemPermissionAccess("deny")
    }
}

@Serializable
class FileSystemPermissionPath internal constructor(
    val type: String,
    val path: CodexHostPath? = null,
    val pattern: String? = null,
    val value: FileSystemSpecialPath? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is FileSystemPermissionPath &&
            type == other.type &&
            path == other.path &&
            pattern == other.pattern &&
            value == other.value

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (path?.hashCode() ?: 0)
        result = 31 * result + (pattern?.hashCode() ?: 0)
        result = 31 * result + (value?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "FileSystemPermissionPath(type=$type, path=$path, pattern=$pattern, value=$value)"

    companion object {
        fun Path(path: CodexHostPath): FileSystemPermissionPath =
            FileSystemPermissionPath(type = "path", path = path)

        fun GlobPattern(pattern: String): FileSystemPermissionPath =
            FileSystemPermissionPath(type = "glob_pattern", pattern = pattern)

        fun Root(): FileSystemPermissionPath =
            Special(FileSystemSpecialPath.Root())

        fun Minimal(): FileSystemPermissionPath =
            Special(FileSystemSpecialPath.Minimal())

        fun ProjectRoots(subpath: String? = null): FileSystemPermissionPath =
            Special(FileSystemSpecialPath.ProjectRoots(subpath))

        fun Tmpdir(): FileSystemPermissionPath =
            Special(FileSystemSpecialPath.Tmpdir())

        fun SlashTmp(): FileSystemPermissionPath =
            Special(FileSystemSpecialPath.SlashTmp())

        fun Unknown(path: String, subpath: String? = null): FileSystemPermissionPath =
            Special(FileSystemSpecialPath.Unknown(path = path, subpath = subpath))

        fun Special(value: FileSystemSpecialPath): FileSystemPermissionPath =
            FileSystemPermissionPath(type = "special", value = value)
    }
}

@Serializable
class FileSystemSpecialPath internal constructor(
    val kind: String,
    val path: String? = null,
    val subpath: String? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is FileSystemSpecialPath &&
            kind == other.kind &&
            path == other.path &&
            subpath == other.subpath

    override fun hashCode(): Int {
        var result = kind.hashCode()
        result = 31 * result + (path?.hashCode() ?: 0)
        result = 31 * result + (subpath?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "FileSystemSpecialPath(kind=$kind, path=$path, subpath=$subpath)"

    companion object {
        fun Root(): FileSystemSpecialPath = FileSystemSpecialPath(kind = "root")

        fun Minimal(): FileSystemSpecialPath = FileSystemSpecialPath(kind = "minimal")

        fun ProjectRoots(subpath: String? = null): FileSystemSpecialPath =
            FileSystemSpecialPath(kind = "project_roots", subpath = subpath)

        fun Tmpdir(): FileSystemSpecialPath = FileSystemSpecialPath(kind = "tmpdir")

        fun SlashTmp(): FileSystemSpecialPath = FileSystemSpecialPath(kind = "slash_tmp")

        fun Unknown(path: String, subpath: String? = null): FileSystemSpecialPath =
            FileSystemSpecialPath(kind = "unknown", path = path, subpath = subpath)
    }
}

@Serializable
data class PermissionNetwork(
    val enabled: Boolean? = null,
)

@Serializable
data class PermissionApprovalResponse(
    val permissions: PermissionProfile,
    val scope: PermissionGrantScope? = null,
    val strictAutoReview: Boolean? = null,
) {
    companion object {
        val Decline = PermissionApprovalResponse(
            permissions = PermissionProfile(),
        )
    }
}

@Serializable
@JvmInline
value class PermissionGrantScope(val value: String) {
    companion object {
        val Turn = PermissionGrantScope("turn")
        val Session = PermissionGrantScope("session")
    }
}

fun interface PermissionApprovalHandler {
    suspend fun decide(request: PermissionApprovalRequest): PermissionApprovalResponse
}
