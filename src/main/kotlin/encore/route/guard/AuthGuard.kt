package encore.route.guard

import io.ktor.server.application.ApplicationCall

/**
 * Authentication mechanism executed before request handling.
 *
 * Implementations inspect the [ApplicationCall] in [verify] to determine
 * whether the request may proceed.
 *
 * Typical use-cases:
 * - Validating cookies or session tokens.
 * - Verifying login state or account identity in server
 *   based on the available information in the request.
 *
 * This should not handle low-level security mitigation or request validation.
 *
 * Can use [NoAuthGuard] to skip this check.
 */
interface AuthGuard {
    /**
     * Verifies the request and returns a [GuardResult].
     */
    suspend fun verify(call: ApplicationCall): GuardResult
}
