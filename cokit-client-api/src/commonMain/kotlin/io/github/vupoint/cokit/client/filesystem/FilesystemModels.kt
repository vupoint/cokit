package io.github.vupoint.cokit.client.filesystem

import io.github.vupoint.cokit.client.CodexHostPath
import kotlinx.serialization.Serializable

/** Caller-supplied connection-scoped subscription id for host filesystem watches. */
@JvmInline
@Serializable
value class FilesystemWatchId(val value: String)

/** Reads a file on the app-server host; [path] does not refer to the Kotlin client's filesystem. */
@Serializable
data class FilesystemReadFileParams(
    val path: CodexHostPath,
)

/** File bytes encoded as base64; the application owns decoding and interpretation of the untrusted contents. */
@Serializable
data class FilesystemReadFileResult(
    val dataBase64: String,
)

/** Requests metadata for a path on the app-server host. */
@Serializable
data class FilesystemGetMetadataParams(
    val path: CodexHostPath,
)

/**
 * Server-reported host path metadata.
 *
 * @property createdAtMs Creation time in Unix epoch milliseconds.
 * @property modifiedAtMs Last modification time in Unix epoch milliseconds.
 */
@Serializable
data class FilesystemGetMetadataResult(
    val isDirectory: Boolean,
    val isFile: Boolean,
    val isSymlink: Boolean,
    val createdAtMs: Long,
    val modifiedAtMs: Long,
)

/** Lists direct children of a directory on the app-server host. */
@Serializable
data class FilesystemReadDirectoryParams(
    val path: CodexHostPath,
)

/** Direct child names and kinds reported by app-server; entries are not absolute paths. */
@Serializable
data class FilesystemReadDirectoryResult(
    val entries: List<FilesystemDirectoryEntry> = emptyList(),
)

/**
 * Writes caller-provided bytes on the app-server host, subject to server permissions.
 *
 * @property dataBase64 Base64-encoded file bytes; CoKit does not encode text automatically.
 */
@Serializable
data class FilesystemWriteFileParams(
    val path: CodexHostPath,
    val dataBase64: String,
)

/** Creates a directory on the app-server host. Null [recursive] leaves the server default in effect. */
@Serializable
data class FilesystemCreateDirectoryParams(
    val path: CodexHostPath,
    val recursive: Boolean? = null,
)

/** Copies host filesystem content between server-side paths. Null [recursive] leaves the server default in effect. */
@Serializable
data class FilesystemCopyParams(
    val sourcePath: CodexHostPath,
    val destinationPath: CodexHostPath,
    val recursive: Boolean? = null,
)

/**
 * Removes a host path through app-server; the application must authorize the destructive action. Null flags leave server defaults in effect.
 */
@Serializable
data class FilesystemRemoveParams(
    val path: CodexHostPath,
    val recursive: Boolean? = null,
    val force: Boolean? = null,
)

/**
 * Starts a connection-scoped host path subscription with a caller-supplied [watchId]. Changed-path notifications do not include file contents.
 */
@Serializable
data class FilesystemWatchParams(
    val path: CodexHostPath,
    val watchId: FilesystemWatchId,
)

/** Canonicalized host path returned for a filesystem subscription. */
@Serializable
data class FilesystemWatchResult(
    val path: CodexHostPath,
)

/** Stops the host filesystem subscription identified by [watchId] on this connection. */
@Serializable
data class FilesystemUnwatchParams(
    val watchId: FilesystemWatchId,
)

/** Direct child name and file kind; [fileName] is relative to the directory that was listed. */
@Serializable
data class FilesystemDirectoryEntry(
    val fileName: String,
    val isDirectory: Boolean,
    val isFile: Boolean,
)
