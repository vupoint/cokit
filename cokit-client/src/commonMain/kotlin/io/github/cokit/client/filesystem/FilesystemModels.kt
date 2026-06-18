package io.github.cokit.client.filesystem

import io.github.cokit.client.CodexHostPath
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class FilesystemWatchId(val value: String)

@Serializable
data class FilesystemReadFileParams(
    val path: CodexHostPath,
)

@Serializable
data class FilesystemReadFileResult(
    val dataBase64: String,
)

@Serializable
data class FilesystemGetMetadataParams(
    val path: CodexHostPath,
)

@Serializable
data class FilesystemGetMetadataResult(
    val isDirectory: Boolean,
    val isFile: Boolean,
    val isSymlink: Boolean,
    val createdAtMs: Long,
    val modifiedAtMs: Long,
)

@Serializable
data class FilesystemReadDirectoryParams(
    val path: CodexHostPath,
)

@Serializable
data class FilesystemReadDirectoryResult(
    val entries: List<FilesystemDirectoryEntry> = emptyList(),
)

@Serializable
data class FilesystemWriteFileParams(
    val path: CodexHostPath,
    val dataBase64: String,
)

@Serializable
data class FilesystemCreateDirectoryParams(
    val path: CodexHostPath,
    val recursive: Boolean? = null,
)

@Serializable
data class FilesystemCopyParams(
    val sourcePath: CodexHostPath,
    val destinationPath: CodexHostPath,
    val recursive: Boolean? = null,
)

@Serializable
data class FilesystemRemoveParams(
    val path: CodexHostPath,
    val recursive: Boolean? = null,
    val force: Boolean? = null,
)

@Serializable
data class FilesystemWatchParams(
    val path: CodexHostPath,
    val watchId: FilesystemWatchId,
)

@Serializable
data class FilesystemWatchResult(
    val path: CodexHostPath,
)

@Serializable
data class FilesystemUnwatchParams(
    val watchId: FilesystemWatchId,
)

@Serializable
data class FilesystemDirectoryEntry(
    val fileName: String,
    val isDirectory: Boolean,
    val isFile: Boolean,
)
