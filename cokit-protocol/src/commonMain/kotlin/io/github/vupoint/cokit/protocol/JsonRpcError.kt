package io.github.vupoint.cokit.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Error payload returned by app-server for a failed JSON-RPC request.
 *
 * @property code Server-defined error code; callers must preserve unknown codes.
 * @property message Human-readable description supplied by the server.
 * @property data Optional structured details preserved as JSON; absent by default.
 */
@Serializable
data class JsonRpcErrorObject(
    val code: Int,
    val message: String,
    val data: JsonElement? = null,
)
