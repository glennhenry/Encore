package encore.routes

import encore.fancam.Fancam
import encore.routes.guard.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.util.date.*

/**
 * Handler for one or more server endpoints.
 *
 * Implementations register routes through the [Route] receiver in [install].
 *
 * Routes may be wrapped with [intercept] to apply:
 * - Request/response logging.
 * - Security checks.
 * - Authentication checks.
 *
 * Example:
 * ```
 * override fun Route.install() {
 *     get("/home") {
 *         intercept(call) {
 *             if (...) {
 *                 return@intercept
 *             }
 *             call.respond(...)
 *         }
 *     }
 *
 *     get("/home/help") {
 *         intercept(call) {
 *             call.respond(...)
 *         }
 *     }
 * }
 * ```
 *
 * Actual routes registration to the Ktor's routing block can be done like:
 * ```
 * routing {
 *     with(AuthRoutes()) {
 *         install()
 *     }
 * }
 * ```
 */
interface RouteHandler {
    /**
     * Human-readable name used for logging purpose.
     */
    val name: String

    /**
     * Whether to enable request and response logging.
     */
    val enableLogging: Boolean

    /**
     * Security guard applied before handling requests.
     * Use [NoSecurityGuard] to disable security checks.
     */
    val security: SecurityGuard

    /**
     * Authentication guard applied before handling requests.
     * Use [NoAuthGuard] to disable authentication checks.
     */
    val auth: AuthGuard

    /**
     * Register routes for this handler.
     */
    fun Route.install()
}

/**
 * Intercepts route handling to:
 * - Log incoming requests when [enableLogging] is `true`.
 * - Apply [security] and [auth] guards before handling.
 * - Abort processing unless both guards return [GuardResult.Welcome].
 * - Log outgoing responses when [enableLogging] is `true`.
 *
 * Returning early within the intercept block is guaranteed
 * to exit the handling immediately.
 *
 * Handler may use [NoSecurityGuard] or [NoAuthGuard] to skip
 * automatic checks when different routes require different handling.
 *
 * This is useful when:
 * - Public and authenticated routes are mixed within the same handler.
 * - Authentication must be decided dynamically per route.
 *
 * In such cases, handler may:
 * - Use empty guards and complete checks manually.
 * - Use non-empty guards, but do not apply [intercept] to all routes.
 *
 * @param call Ktor request representation.
 * @param block Request handling block.
 */
suspend fun RouteHandler.intercept(call: ApplicationCall, block: suspend () -> Unit) {
    if (enableLogging) {
        val startedAt = getTimeMillis()
        call.attributes.put(ReqResLoggingKey, startedAt)

        Fancam.debug { call.stringifyHttpRequest(unhandled = false) }
    }

    try {
        val result = security.verify(call)
        if (result is GuardResult.GetOut) {
            Fancam.trace { "Request refused by security due to: ${result.why}" }
            return
        }
    } catch (e: Throwable) {
        Fancam.error(e) { "Error on security verify of $name at .../..." }
        return
    }

    try {
        val result = auth.verify(call)
        if (result is GuardResult.GetOut) {
            Fancam.trace { "Request refused by authentication due to: ${result.why}" }
            return
        }
    } catch (e: Throwable) {
        Fancam.error(e) { "Error on auth verify of $name at .../..." }
        return
    }

    block()
}
