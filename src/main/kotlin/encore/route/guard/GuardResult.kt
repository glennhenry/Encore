package encore.route.guard

import bootstrap.errorHtml
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall

/**
 * The result of pre-request handling mechanism
 * returned by [SecurityGuard] and [AuthGuard].
 *
 * - [GuardResult.Welcome]
 * - [GuardResult.GetOut]
 * - [GuardResult.Reject]
 */
sealed interface GuardResult {
    /**
     * The request is allowed to continue.
     */
    data object Welcome : GuardResult

    /**
     * The request is rejected and the call will be responded
     * directly using [errorHtml] with the [status] code and string [message].
     *
     * @param reason Optional message for logging purpose.
     */
    data class GetOut(val status: HttpStatusCode, val message: String, val reason: String? = null) : GuardResult

    /**
     * The request is simply rejected with no guaranteed response.
     *
     * **The guard implementation is expected to call [ApplicationCall.respond] manually**,
     * otherwise nothing will be used as response for the request.
     *
     * @param reason Optional message for logging purpose.
     */
    data class Reject(val reason: String? = null) : GuardResult
}
