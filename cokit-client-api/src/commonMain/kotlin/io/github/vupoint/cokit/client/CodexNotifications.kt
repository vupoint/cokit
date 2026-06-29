package io.github.vupoint.cokit.client

import io.github.vupoint.cokit.client.auth.LoginAccountId
import io.github.vupoint.cokit.client.auth.AccountRateLimitSnapshot
import io.github.vupoint.cokit.client.commands.CommandExecOutputStream
import io.github.vupoint.cokit.client.commands.CommandProcessId
import io.github.vupoint.cokit.client.filesystem.FilesystemWatchId
import io.github.vupoint.cokit.client.remote.RemoteControlStatusSnapshot
import kotlinx.serialization.Serializable

sealed interface CodexNotification {
    val method: String

    data class ThreadStarted(
        val threadId: ThreadId,
        val thread: Thread? = null,
    ) : CodexNotification {
        override val method: String = "thread/started"
    }

    data class ThreadStatusChanged(
        val threadId: ThreadId,
        val status: ThreadStatusType,
    ) : CodexNotification {
        override val method: String = "thread/status/changed"
    }

    data class ThreadTokenUsageUpdated(
        val threadId: ThreadId,
        val turnId: TurnId,
        val tokenUsage: ThreadTokenUsage,
    ) : CodexNotification {
        override val method: String = "thread/tokenUsage/updated"
    }

    data class TurnStarted(
        val turn: Turn,
    ) : CodexNotification {
        override val method: String = "turn/started"
    }

    data class TurnCompleted(
        val turn: Turn,
    ) : CodexNotification {
        override val method: String = "turn/completed"
    }

    data class TurnFailed(
        val turn: Turn,
    ) : CodexNotification {
        override val method: String = "turn/completed"
    }

    data class ItemStarted(
        val threadId: ThreadId,
        val turnId: TurnId,
        val item: ThreadItemSummary,
    ) : CodexNotification {
        override val method: String = "item/started"
    }

    data class ItemCompleted(
        val threadId: ThreadId,
        val turnId: TurnId,
        val item: ThreadItemSummary,
    ) : CodexNotification {
        override val method: String = "item/completed"
    }

    data class AgentMessageDelta(
        val threadId: ThreadId,
        val turnId: TurnId,
        val itemId: ItemId,
        val delta: String,
    ) : CodexNotification {
        override val method: String = "item/agentMessage/delta"
    }

    data class ReasoningSummaryTextDelta(
        val threadId: ThreadId,
        val turnId: TurnId,
        val itemId: ItemId,
        val summaryIndex: Int,
        val delta: String,
    ) : CodexNotification {
        override val method: String = "item/reasoning/summaryTextDelta"
    }

    data class Warning(
        val threadId: ThreadId? = null,
        val message: String,
    ) : CodexNotification {
        override val method: String = "warning"
    }

    data class ConfigWarning(
        val summary: String,
        val details: String? = null,
        val path: String? = null,
        val range: ConfigTextRange? = null,
    ) : CodexNotification {
        override val method: String = "configWarning"
    }

    data class Error(
        val threadId: ThreadId,
        val turnId: TurnId,
        val error: CodexNotificationError,
        val willRetry: Boolean,
    ) : CodexNotification {
        override val method: String = "error"
    }

    data class ServerRequestResolved(
        val threadId: ThreadId,
        val requestId: CodexServerRequestId,
    ) : CodexNotification {
        override val method: String = "serverRequest/resolved"
    }

    data class CommandExecOutputDelta(
        val processId: CommandProcessId,
        val stream: CommandExecOutputStream,
        val deltaBase64: String,
        val capReached: Boolean,
    ) : CodexNotification {
        override val method: String = "command/exec/outputDelta"
    }

    data class FilesystemChanged(
        val watchId: FilesystemWatchId,
        val changedPaths: List<CodexHostPath>,
    ) : CodexNotification {
        override val method: String = "fs/changed"
    }

    data class AccountLoginCompleted(
        val loginId: LoginAccountId? = null,
        val success: Boolean,
        val error: String? = null,
    ) : CodexNotification {
        override val method: String = "account/login/completed"
    }

    data class AccountRateLimitsUpdated(
        val rateLimits: AccountRateLimitSnapshot,
    ) : CodexNotification {
        override val method: String = "account/rateLimits/updated"
    }

    @ExperimentalCodexApi
    data class RemoteControlStatusChanged(
        val status: RemoteControlStatusSnapshot,
    ) : CodexNotification {
        override val method: String = "remoteControl/status/changed"
    }

    data class Unknown(
        override val method: String,
    ) : CodexNotification
}

@Serializable
data class ThreadTokenUsage(
    val total: TokenUsageBreakdown,
    val last: TokenUsageBreakdown,
    val modelContextWindow: Long? = null,
)

@Serializable
data class TokenUsageBreakdown(
    val totalTokens: Long,
    val inputTokens: Long,
    val cachedInputTokens: Long,
    val outputTokens: Long,
    val reasoningOutputTokens: Long,
)

@Serializable
data class ConfigTextPosition(
    val line: Int,
    val column: Int,
)

@Serializable
data class ConfigTextRange(
    val start: ConfigTextPosition,
    val end: ConfigTextPosition,
)

@Serializable
data class CodexNotificationError(
    val message: String,
    val additionalDetails: String? = null,
)

sealed interface CodexServerRequestId {
    data class Number(val value: Long) : CodexServerRequestId

    data class StringId(val value: String) : CodexServerRequestId
}
