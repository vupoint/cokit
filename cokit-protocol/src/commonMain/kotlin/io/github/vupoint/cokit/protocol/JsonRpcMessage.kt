package io.github.vupoint.cokit.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * App-server JSON-RPC envelope without a `jsonrpc` version field.
 *
 * [JsonRpcMessageSerializer] selects the envelope type from its fields. Method-specific
 * parameters and results remain JSON so unrecognized payload fields can be retained.
 */
@Serializable(with = JsonRpcMessageSerializer::class)
sealed interface JsonRpcMessage

/**
 * Request sent by either peer and correlated with a response by [id].
 *
 * @property id Numeric or string identifier whose JSON kind must be preserved in the response.
 * @property method App-server method name, passed through without method validation.
 * @property params Optional method-specific JSON payload; absent by default.
 */
@Serializable
data class JsonRpcRequest(
    val id: JsonRpcId,
    val method: String,
    val params: JsonElement? = null,
) : JsonRpcMessage

/**
 * One-way message that has no request identifier and expects no response.
 *
 * @property method App-server notification name, passed through without method validation.
 * @property params Optional notification payload; absent by default.
 */
@Serializable
data class JsonRpcNotification(
    val method: String,
    val params: JsonElement? = null,
) : JsonRpcMessage

/**
 * Response carrying a result or error for a previously sent request.
 *
 * Construction does not enforce mutually exclusive [result] and [error] values;
 * decoding through [JsonRpcMessageSerializer] rejects envelopes containing both fields.
 *
 * @property id Identifier copied from the request, including its JSON kind.
 * @property result Optional successful JSON result; `null` by default.
 * @property error Optional failure details; `null` by default.
 */
@Serializable
data class JsonRpcResponse(
    val id: JsonRpcId,
    val result: JsonElement? = null,
    val error: JsonRpcErrorObject? = null,
) : JsonRpcMessage

/** A request identifier that preserves the distinction between JSON strings and integers. */
@Serializable(with = JsonRpcIdSerializer::class)
sealed interface JsonRpcId {
    /**
     * Integer identifier represented as a JSON number.
     *
     * @property value Signed 64-bit identifier; numeric decoding rejects values outside this range.
     */
    @JvmInline
    value class Number(val value: Long) : JsonRpcId

    /**
     * String identifier, including strings containing only digits.
     *
     * @property value Identifier text encoded as a JSON string.
     */
    @JvmInline
    value class StringId(val value: String) : JsonRpcId
}
