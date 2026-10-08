package io.github.vupoint.cokit.client.approvals

import io.github.vupoint.cokit.client.CodexHostPath
import io.github.vupoint.cokit.client.ItemId
import io.github.vupoint.cokit.client.ThreadId
import io.github.vupoint.cokit.client.TurnId
import kotlinx.serialization.Serializable

/**
 * Server request for additional permissions in a turn; the requested profile must be evaluated before granting it.
 *
 * @property startedAtMs Server-reported request start time in Unix epoch milliseconds.
 * @property cwd Working directory on the app-server host.
 * @property permissions Requested permissions, not permissions already granted by the client.
 */
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

/** Server environment identifier accompanying a permission request, separate from a thread id. */
@Serializable
@JvmInline
value class PermissionEnvironmentId(val value: String)

/**
 * Filesystem and network permissions requested or granted together. An empty profile is used by CoKit to decline additional access.
 *
 * @property fileSystem Null leaves this category unspecified.
 * @property network Null leaves this category unspecified.
 */
@Serializable
data class PermissionProfile(
    val fileSystem: PermissionFileSystem? = null,
    val network: PermissionNetwork? = null,
)

/**
 * Filesystem access profile interpreted by app-server; explicit entries coexist with legacy read and write lists.
 *
 * @property globScanMaxDepth Optional maximum directory scan depth for glob permission matching.
 * @property read Legacy host paths requesting read access.
 * @property write Legacy host paths requesting write access.
 */
@Serializable
data class PermissionFileSystem(
    val entries: List<FileSystemPermissionEntry>? = null,
    val globScanMaxDepth: Int? = null,
    val read: List<CodexHostPath>? = null,
    val write: List<CodexHostPath>? = null,
)

/** An access decision applied to a concrete, glob, or server-defined special filesystem target. */
@Serializable
data class FileSystemPermissionEntry(
    val access: FileSystemPermissionAccess,
    val path: FileSystemPermissionPath,
)

/** Filesystem access rule, including explicit deny; unknown wire strings are retained. */
@Serializable
@JvmInline
value class FileSystemPermissionAccess(val value: String) {
    companion object {
        val Read = FileSystemPermissionAccess("read")
        val Write = FileSystemPermissionAccess("write")
        val Deny = FileSystemPermissionAccess("deny")
    }
}

/**
 * Tagged filesystem target. Use the companion factories to keep the discriminator and its payload consistent.
 *
 * @property type Wire discriminator selecting path, glob pattern, or special target.
 * @property path Host path for the concrete-path variant.
 * @property pattern Glob text for the glob-pattern variant.
 * @property value Server-defined target for the special variant.
 */
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
        /** Targets a concrete path on the app-server host. */
        fun Path(path: CodexHostPath): FileSystemPermissionPath =
            FileSystemPermissionPath(type = "path", path = path)

        /** Targets host paths matching a glob interpreted by app-server. */
        fun GlobPattern(pattern: String): FileSystemPermissionPath =
            FileSystemPermissionPath(type = "glob_pattern", pattern = pattern)

        /** Selects the server-defined filesystem root target. */
        fun Root(): FileSystemPermissionPath =
            Special(FileSystemSpecialPath.Root())

        /** Selects the server-defined minimal filesystem target. */
        fun Minimal(): FileSystemPermissionPath =
            Special(FileSystemSpecialPath.Minimal())

        /** Selects server project roots, optionally restricted to a relative subpath. */
        fun ProjectRoots(subpath: String? = null): FileSystemPermissionPath =
            Special(FileSystemSpecialPath.ProjectRoots(subpath))

        /** Selects the temporary directory target resolved by app-server. */
        fun Tmpdir(): FileSystemPermissionPath =
            Special(FileSystemSpecialPath.Tmpdir())

        /** Selects the server-defined /tmp target. */
        fun SlashTmp(): FileSystemPermissionPath =
            Special(FileSystemSpecialPath.SlashTmp())

        /** Preserves an unknown special target name and optional relative subpath. */
        fun Unknown(path: String, subpath: String? = null): FileSystemPermissionPath =
            Special(FileSystemSpecialPath.Unknown(path = path, subpath = subpath))

        /** Wraps a server-defined special target in the permission-path union. */
        fun Special(value: FileSystemSpecialPath): FileSystemPermissionPath =
            FileSystemPermissionPath(type = "special", value = value)
    }
}

/**
 * Named filesystem target resolved on the app-server host, such as project roots or temporary directories.
 *
 * @property kind Wire discriminator for the special target.
 * @property path Original target name for the unknown variant.
 * @property subpath Optional relative suffix within project roots or an unknown target.
 */
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
        /** Selects the server-defined filesystem root target. */
        fun Root(): FileSystemSpecialPath = FileSystemSpecialPath(kind = "root")

        /** Selects the server-defined minimal filesystem target. */
        fun Minimal(): FileSystemSpecialPath = FileSystemSpecialPath(kind = "minimal")

        /** Selects server project roots, optionally restricted to a relative subpath. */
        fun ProjectRoots(subpath: String? = null): FileSystemSpecialPath =
            FileSystemSpecialPath(kind = "project_roots", subpath = subpath)

        /** Selects the temporary directory target resolved by app-server. */
        fun Tmpdir(): FileSystemSpecialPath = FileSystemSpecialPath(kind = "tmpdir")

        /** Selects the server-defined /tmp target. */
        fun SlashTmp(): FileSystemSpecialPath = FileSystemSpecialPath(kind = "slash_tmp")

        /** Preserves an unknown special target name and optional relative subpath. */
        fun Unknown(path: String, subpath: String? = null): FileSystemSpecialPath =
            FileSystemSpecialPath(kind = "unknown", path = path, subpath = subpath)
    }
}

/**
 * Network permission adjustment. A null [enabled] leaves the requested state unspecified rather than explicitly disabling access.
 */
@Serializable
data class PermissionNetwork(
    val enabled: Boolean? = null,
)

/**
 * Permissions explicitly granted by an application handler. [Decline] returns an empty profile, granting no additional access.
 *
 * @property scope Optional lifetime of the grant, interpreted by app-server.
 * @property strictAutoReview Optional strict automatic review setting passed to app-server.
 */
@Serializable
data class PermissionApprovalResponse(
    val permissions: PermissionProfile,
    val scope: PermissionGrantScope? = null,
    val strictAutoReview: Boolean? = null,
) {
    companion object {
        /** Grants no additional filesystem or network access by returning an empty profile. */
        val Decline = PermissionApprovalResponse(
            permissions = PermissionProfile(),
        )
    }
}

/** Lifetime of an approved permission grant, typically one turn or the session; unknown strings are retained. */
@Serializable
@JvmInline
value class PermissionGrantScope(val value: String) {
    companion object {
        val Turn = PermissionGrantScope("turn")
        val Session = PermissionGrantScope("session")
    }
}

/**
 * Application policy callback for additional permissions. Without a handler, CoKit responds with [PermissionApprovalResponse.Decline].
 */
fun interface PermissionApprovalHandler {
    /** Returns the permissions actually granted, which may be narrower than the requested profile. */
    suspend fun decide(request: PermissionApprovalRequest): PermissionApprovalResponse
}
