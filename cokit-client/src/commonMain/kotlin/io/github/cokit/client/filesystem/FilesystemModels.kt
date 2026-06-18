package io.github.cokit.client.filesystem

import io.github.cokit.client.CodexHostPath
import kotlinx.serialization.Serializable

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
data class FilesystemDirectoryEntry(
    val fileName: String,
    val isDirectory: Boolean,
    val isFile: Boolean,
)
