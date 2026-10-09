package io.github.vupoint.cokit.client.approvals

import io.github.vupoint.cokit.client.CodexHostPath
import io.github.vupoint.cokit.client.CodexJsonPayload
import io.github.vupoint.cokit.client.ItemId
import io.github.vupoint.cokit.client.ThreadId
import io.github.vupoint.cokit.client.TurnId
import io.github.vupoint.cokit.client.environment.EnvironmentId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Server-initiated authorization request for a command execution item or stdin write. Optional details are server-reported context, not a guarantee that an action is safe.
 *
 * @property startedAtMs Server-reported request start time in Unix epoch milliseconds.
 * @property kind Distinguishes command execution from writing to an existing process.
 * @property proposedExecpolicyAmendment Suggested command-prefix policy amendment, requiring an explicit policy decision.
 * @property proposedNetworkPolicyAmendments Suggested host rules; inspecting them does not install them.
 */
@Serializable
data class CommandApprovalRequest(
    val threadId: ThreadId,
    val turnId: TurnId,
    val itemId: ItemId,
    val startedAtMs: Long,
    val kind: CommandApprovalKind = CommandApprovalKind.Command,
    val approvalId: CommandApprovalId? = null,
    val command: String? = null,
    val commandActions: List<CommandAction> = emptyList(),
    val cwd: CodexHostPath? = null,
    val environmentId: EnvironmentId? = null,
    val networkApprovalContext: NetworkApprovalContext? = null,
    val proposedExecpolicyAmendment: List<String>? = null,
    val proposedNetworkPolicyAmendments: List<NetworkPolicyAmendment>? = null,
    val reason: String? = null,
) {
    override fun toString(): String =
        "CommandApprovalRequest(" +
            "threadId=$threadId, " +
            "turnId=$turnId, " +
            "itemId=$itemId, " +
            "startedAtMs=$startedAtMs, " +
            "kind=$kind, " +
            "hasApprovalId=${approvalId != null}, " +
            "hasCommand=${command != null}, " +
            "commandActionCount=${commandActions.size}, " +
            "hasCwd=${cwd != null}, " +
            "hasEnvironmentId=${environmentId != null}, " +
            "hasNetworkApprovalContext=${networkApprovalContext != null}, " +
            "execpolicyAmendmentCount=${proposedExecpolicyAmendment?.size ?: 0}, " +
            "networkPolicyAmendmentCount=${proposedNetworkPolicyAmendments?.size ?: 0}, " +
            "hasReason=${reason != null})"
}

/** The action being approved; unknown values remain available to explicit handlers. */
@Serializable
@JvmInline
value class CommandApprovalKind(val value: String) {
    companion object {
        val Command = CommandApprovalKind("command")
        val WriteStdin = CommandApprovalKind("writeStdin")
    }
}

/** Server identifier for one approval within a command item, separate from the JSON-RPC request id. */
@Serializable
@JvmInline
value class CommandApprovalId(val value: String)

/** Network destination presented for approval; the application must evaluate the host and protocol together. */
@Serializable
data class NetworkApprovalContext(
    val host: String,
    val protocol: NetworkApprovalProtocol,
)

/** Server-reported network protocol; unfamiliar strings remain readable for policy handlers. */
@Serializable
@JvmInline
value class NetworkApprovalProtocol(val value: String) {
    companion object {
        val Http = NetworkApprovalProtocol("http")
        val Https = NetworkApprovalProtocol("https")
        val Socks5Tcp = NetworkApprovalProtocol("socks5Tcp")
        val Socks5Udp = NetworkApprovalProtocol("socks5Udp")
    }
}

/** Proposed network host rule carried in a command approval request. */
@Serializable
data class NetworkPolicyAmendment(
    val action: NetworkPolicyRuleAction,
    val host: String,
)

/** Allow or deny action for a network rule; unknown wire strings are retained. */
@Serializable
@JvmInline
value class NetworkPolicyRuleAction(val value: String) {
    companion object {
        val Allow = NetworkPolicyRuleAction("allow")
        val Deny = NetworkPolicyRuleAction("deny")
    }
}

/**
 * Server interpretation of command intent for an approval prompt. Classification is descriptive and does not authorize execution; future variants retain their JSON in [Custom].
 */
@Serializable(with = CommandActionSerializer::class)
sealed interface CommandAction {
    /** Server-classified file read, including the displayed name and host path. */
    data class Read(
        val command: String,
        val name: String,
        val path: CodexHostPath,
    ) : CommandAction

    /** Server-classified directory listing with an optional reported path. */
    data class ListFiles(
        val command: String,
        val path: String? = null,
    ) : CommandAction

    /** Server-classified search with optional path and query details. */
    data class Search(
        val command: String,
        val path: String? = null,
        val query: String? = null,
    ) : CommandAction

    /** Command the server did not classify as a known action. */
    data class Unknown(
        val command: String,
    ) : CommandAction

    /** Unrecognized action variant preserved as its complete JSON payload; handlers must evaluate it explicitly. */
    data class Custom(
        val payload: CodexJsonPayload,
    ) : CommandAction
}

internal object CommandActionSerializer : KSerializer<CommandAction> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): CommandAction {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("CommandAction requires JSON decoding")
        val element = jsonDecoder.decodeJsonElement()
        val value = element.jsonObject
        val command = value.requiredString("command")
        return when (value.requiredString("type")) {
            "read" -> CommandAction.Read(
                command = command,
                name = value.requiredString("name"),
                path = CodexHostPath(value.requiredString("path")),
            )
            "listFiles" -> CommandAction.ListFiles(
                command = command,
                path = value.optionalString("path"),
            )
            "search" -> CommandAction.Search(
                command = command,
                path = value.optionalString("path"),
                query = value.optionalString("query"),
            )
            "unknown" -> CommandAction.Unknown(command)
            else -> CommandAction.Custom(CodexJsonPayload(element))
        }
    }

    override fun serialize(encoder: Encoder, value: CommandAction) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("CommandAction requires JSON encoding")
        val element = when (value) {
            is CommandAction.Read -> buildJsonObject {
                put("type", "read")
                put("command", value.command)
                put("name", value.name)
                put("path", value.path.value)
            }
            is CommandAction.ListFiles -> buildJsonObject {
                put("type", "listFiles")
                put("command", value.command)
                value.path?.let { put("path", it) }
            }
            is CommandAction.Search -> buildJsonObject {
                put("type", "search")
                put("command", value.command)
                value.path?.let { put("path", it) }
                value.query?.let { put("query", it) }
            }
            is CommandAction.Unknown -> buildJsonObject {
                put("type", "unknown")
                put("command", value.command)
            }
            is CommandAction.Custom -> value.payload.element
        }
        jsonEncoder.encodeJsonElement(element)
    }
}

private fun JsonObject.requiredString(name: String): String {
    return this[name]?.jsonPrimitive?.contentOrNull
        ?: throw SerializationException("CommandAction requires $name")
}

private fun JsonObject.optionalString(name: String): String? {
    val value = this[name] ?: return null
    return (value as? JsonPrimitive)?.contentOrNull
}
