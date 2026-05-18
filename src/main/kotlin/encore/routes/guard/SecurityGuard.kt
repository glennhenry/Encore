package encore.routes.guard

import io.ktor.server.application.ApplicationCall

/**
 * Security mechanism executed before request handling.
 *
 * Implementations inspect the [ApplicationCall] in [verify] to determine
 * whether the request may proceed.
 *
 * Typical use-cases:
 * - Rate limiting.
 * - IP address filtering or banning.
 * - Mitigating malformed or abusive requests.
 *
 * This should not handle authentication or application-level validation.
 */
interface SecurityGuard {
    /**
     * Verify the following request, returning a [GuardResult].
     */
    suspend fun verify(call: ApplicationCall): GuardResult
}
