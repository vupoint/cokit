package io.github.vupoint.cokit.client.server

import io.github.vupoint.cokit.client.ItemId
import io.github.vupoint.cokit.client.ThreadId
import io.github.vupoint.cokit.client.TurnId
import kotlinx.serialization.Serializable

/**
 * Server request for application-mediated user answers. CoKit cancels it when no handler is registered.
 *
 * @property autoResolutionMs Optional server-provided automatic resolution interval in milliseconds.
 */
@Serializable
data class UserInputRequest(
    val threadId: ThreadId,
    val turnId: TurnId,
    val itemId: ItemId,
    val questions: List<UserInputQuestion>,
    val autoResolutionMs: Long? = null,
)

/**
 * One user-facing question reported by app-server.
 *
 * @property options Optional offered choices; null supplies no choice list.
 * @property isOther Whether the question offers an additional free-form answer.
 * @property isSecret Whether answers must be handled as secret input, including in UI and logging.
 */
@Serializable
data class UserInputQuestion(
    val id: UserInputQuestionId,
    val header: String,
    val question: String,
    val options: List<UserInputOption>? = null,
    val isOther: Boolean = false,
    val isSecret: Boolean = false,
)

/** Question key used to correlate answers within a server user-input request. */
@Serializable
@JvmInline
value class UserInputQuestionId(val value: String)

/** Display label and explanation of one user-input choice; answers use the label rather than an option index. */
@Serializable
data class UserInputOption(
    val label: String,
    val description: String,
)

/** Answer strings associated with one question; use [choice] for a label or [freeForm] for entered text. */
@Serializable
data class UserInputAnswer(
    val answers: List<String>,
) {
    companion object {
        /** Answers with the label supplied by the selected option, rather than its position. */
        fun choice(label: String): UserInputAnswer = UserInputAnswer(listOf(label))

        /** Answers with explicitly entered text; secret input must remain private when stored or displayed. */
        fun freeForm(text: String): UserInputAnswer = UserInputAnswer(listOf(text))
    }
}

/** Explicit user-input response, either cancellation or a map of answers keyed by question id. */
sealed interface UserInputResponse {
    /** Cancels user input without supplying answers; used when no application handler is registered. */
    data object Cancel : UserInputResponse

    /** Supplies explicit answers keyed by their original server question ids. */
    data class Answers(
        val answers: Map<UserInputQuestionId, UserInputAnswer>,
    ) : UserInputResponse
}

/** Application callback for user input. Without a handler, CoKit sends [UserInputResponse.Cancel] and invents no answers. */
fun interface UserInputRequestHandler {
    /** Obtains explicit user answers or cancellation; the request alone does not authorize inventing responses. */
    suspend fun respond(request: UserInputRequest): UserInputResponse
}
