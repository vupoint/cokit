package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.auth.LoginAccountId
import io.github.vupoint.cokit.client.auth.AccountRateLimitSnapshot
import io.github.vupoint.cokit.client.commands.CommandExecOutputStream
import io.github.vupoint.cokit.client.commands.CommandProcessId
import io.github.vupoint.cokit.client.filesystem.FilesystemWatchId
import io.github.vupoint.cokit.client.remote.RemoteControlStatusSnapshot
import kotlinx.serialization.Serializable

/**
 * Typed view of an unsolicited server notification.
 *
 * [method] is the upstream notification name. Unknown methods and payloads that fail typed
 * decoding become [Unknown]. This typed stream does not retain their original JSON-RPC parameters.
 */
sealed interface CodexNotification {
    val method: String

    /** Experimental project change signal; re-read the project when its current metadata is needed. */
    @ExperimentalCodexApi
    @Serializable
    data class ProjectChanged(
        val projectId: String,
        val changeType: ProjectChangeType,
    ) : CodexNotification {
        override val method: String = "project/changed"
    }

    /** Experimental thread-to-project association update; null [projectId] clears the association. */
    @ExperimentalCodexApi
    @Serializable
    data class ThreadProjectUpdated(
        val threadId: ThreadId,
        val projectId: String?,
    ) : CodexNotification {
        override val method: String = "thread/project/updated"
    }

    /** Experimental queue change signal; list the queue to obtain its current submissions. */
    @ExperimentalCodexApi
    @Serializable
    data class ThreadQueueChanged(
        val threadId: ThreadId,
    ) : CodexNotification {
        override val method: String = "thread/queue/changed"
    }


    /** Thread-start event with the identifier and any thread snapshot supplied by the server. */
    data class ThreadStarted(
        val threadId: ThreadId,
        val thread: Thread? = null,
    ) : CodexNotification {
        override val method: String = "thread/started"
    }

    /** New runtime status for the identified thread. */
    data class ThreadStatusChanged(
        val threadId: ThreadId,
        val status: ThreadStatus,
    ) : CodexNotification {
        override val method: String = "thread/status/changed"
    }

    /** Updated token counters associated with the identified thread and turn. */
    data class ThreadTokenUsageUpdated(
        val threadId: ThreadId,
        val turnId: TurnId,
        val tokenUsage: ThreadTokenUsage,
    ) : CodexNotification {
        override val method: String = "thread/tokenUsage/updated"
    }

    /** Initial turn lifecycle event; it does not indicate that the turn has completed. */
    data class TurnStarted(
        val turn: Turn,
    ) : CodexNotification {
        override val method: String = "turn/started"
    }

    /** Terminal notification; inspect [turn] status and error before treating it as success. */
    data class TurnCompleted(
        val turn: Turn,
    ) : CodexNotification {
        override val method: String = "turn/completed"
    }

    /** Failed terminal turn decoded from upstream `turn/completed` when its status is `failed`. */
    data class TurnFailed(
        val turn: Turn,
    ) : CodexNotification {
        override val method: String = "turn/completed"
    }

    /** Start event carrying the typed projection of an item in the identified turn. */
    data class ItemStarted(
        val threadId: ThreadId,
        val turnId: TurnId,
        val item: ThreadItemSummary,
    ) : CodexNotification {
        override val method: String = "item/started"
    }

    /** Terminal item event; inspect [item] status before treating execution as successful. */
    data class ItemCompleted(
        val threadId: ThreadId,
        val turnId: TurnId,
        val item: ThreadItemSummary,
    ) : CodexNotification {
        override val method: String = "item/completed"
    }

    /** Incremental agent text for one item; append [delta] to that item's accumulated text. */
    data class AgentMessageDelta(
        val threadId: ThreadId,
        val turnId: TurnId,
        val itemId: ItemId,
        val delta: String,
    ) : CodexNotification {
        override val method: String = "item/agentMessage/delta"
    }

    /** Incremental reasoning-summary text identified by item and [summaryIndex]. */
    data class ReasoningSummaryTextDelta(
        val threadId: ThreadId,
        val turnId: TurnId,
        val itemId: ItemId,
        val summaryIndex: Int,
        val delta: String,
    ) : CodexNotification {
        override val method: String = "item/reasoning/summaryTextDelta"
    }

    /** Server warning optionally associated with a thread; null [threadId] denotes no supplied association. */
    data class Warning(
        val threadId: ThreadId? = null,
        val message: String,
    ) : CodexNotification {
        override val method: String = "warning"
    }

    /** Configuration warning with any source path and text range supplied by the server. */
    data class ConfigWarning(
        val summary: String,
        val details: String? = null,
        val path: String? = null,
        val range: ConfigTextRange? = null,
    ) : CodexNotification {
        override val method: String = "configWarning"
    }

    /**
     * Turn error event with the server's retry decision.
     *
     * [willRetry] indicates server retry intent; it is not a request for the client to retry.
     */
    data class Error(
        val threadId: ThreadId,
        val turnId: TurnId,
        val error: CodexNotificationError,
        val willRetry: Boolean,
    ) : CodexNotification {
        override val method: String = "error"
    }

    /** Indicates that the server considers the correlated server request resolved. */
    data class ServerRequestResolved(
        val threadId: ThreadId,
        val requestId: CodexServerRequestId,
    ) : CodexNotification {
        override val method: String = "serverRequest/resolved"
    }

    /**
     * Incremental command output bytes, encoded in [deltaBase64].
     *
     * [stream] distinguishes output streams. [capReached] reports the server output cap.
     */
    data class CommandExecOutputDelta(
        val processId: CommandProcessId,
        val stream: CommandExecOutputStream,
        val deltaBase64: String,
        val capReached: Boolean,
    ) : CodexNotification {
        override val method: String = "command/exec/outputDelta"
    }

    /** Changed server-host paths for a filesystem watch subscription. */
    data class FilesystemChanged(
        val watchId: FilesystemWatchId,
        val changedPaths: List<CodexHostPath>,
    ) : CodexNotification {
        override val method: String = "fs/changed"
    }

    /** Login completion event; inspect [success] and any [error] before using the account. */
    data class AccountLoginCompleted(
        val loginId: LoginAccountId? = null,
        val success: Boolean,
        val error: String? = null,
    ) : CodexNotification {
        override val method: String = "account/login/completed"
    }

    /** Latest rate-limit snapshot pushed by the server. */
    data class AccountRateLimitsUpdated(
        val rateLimits: AccountRateLimitSnapshot,
    ) : CodexNotification {
        override val method: String = "account/rateLimits/updated"
    }

    /** Experimental remote-control status update. */
    @ExperimentalCodexApi
    data class RemoteControlStatusChanged(
        val status: RemoteControlStatusSnapshot,
    ) : CodexNotification {
        override val method: String = "remoteControl/status/changed"
    }

    /** Attachment reference change identified by type, identity key, and server attachment ID. */
    @Serializable
    data class ThreadAttachmentUpdated(
        val threadId: ThreadId,
        val attachmentId: ThreadAttachmentId,
        val attachmentType: String,
        val identityKey: String,
        val operation: ThreadAttachmentOperation,
    ) : CodexNotification {
        override val method: String get() = "thread/attachment/updated"
    }

    /**
     * Gateway OAuth provider status update with any authorization URL or error supplied by the server.
     *
     * Its string representation omits provider, URL, and error contents.
     */
    @Serializable
    data class GatewayOAuthChanged(
        val providerId: String,
        val status: io.github.vupoint.cokit.client.auth.GatewayOAuthStatus,
        val authUrl: String? = null,
        val error: String? = null,
    ) : CodexNotification {
        override val method: String get() = "account/gatewayOAuth/changed"
        override fun toString(): String = "GatewayOAuthChanged(status=$status, hasAuthUrl=${authUrl != null}, hasError=${error != null})"
    }

    /**
     * Notification not represented by a typed variant, including failed typed decoding.
     *
     * Only [method] is retained; this typed view does not expose the original parameters.
     */
    data class Unknown(
        override val method: String,
    ) : CodexNotification
}

/**
 * Token accounting reported for a thread and the latest usage measurement.
 *
 * @property total Cumulative token usage.
 * @property last Latest token usage measurement reported by the server.
 * @property modelContextWindow Optional model context-window size in tokens.
 */
@Serializable
data class ThreadTokenUsage(
    val total: TokenUsageBreakdown,
    val last: TokenUsageBreakdown,
    val modelContextWindow: Long? = null,
)

/** Server-reported token counts split by input, output, cache, and reasoning categories. */
@Serializable
data class TokenUsageBreakdown(
    val totalTokens: Long,
    val inputTokens: Long,
    val cachedInputTokens: Long,
    val outputTokens: Long,
    val reasoningOutputTokens: Long,
)

/** Server-supplied line and column position in configuration text. */
@Serializable
data class ConfigTextPosition(
    val line: Int,
    val column: Int,
)

/** Server-supplied start and end positions for a configuration warning. */
@Serializable
data class ConfigTextRange(
    val start: ConfigTextPosition,
    val end: ConfigTextPosition,
)

/** Human-readable error message and any additional server details in an error notification. */
@Serializable
data class CodexNotificationError(
    val message: String,
    val additionalDetails: String? = null,
)

/** Identity of an incoming JSON-RPC request, used to correlate server-request resolution events. */
sealed interface CodexServerRequestId {
    /** Incoming request ID represented as a JSON integer. */
    data class Number(val value: Long) : CodexServerRequestId

    /** Incoming request ID represented as a JSON string. */
    data class StringId(val value: String) : CodexServerRequestId
}
