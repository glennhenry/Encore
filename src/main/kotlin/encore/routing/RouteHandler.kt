package encore.routing

import encore.fancam.Fancam
import encore.routing.guard.AuthGuard
import encore.routing.guard.GuardResult
import encore.routing.guard.NoAuthGuard
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.date.*

/**
 * Handler for one or more server endpoints.
 *
 * Implementations register routes through the [Route] receiver in [install].
 *
 * Routes handling can be wrapped with [guard] to apply authentication guard.
 * Alternatively, wrap with [intercept] to also log the request/response.
 *
 * Example:
 * ```
 * override fun Route.install() {
 *     get("/home") {
 *         handle(call, NoAuthGuard) {
 *             if (...) {
 *                 return@handle
 *             }
 *             call.respond(...)
 *         }
 *     }
 *
 *     get("/home/help") {
 *         handle(call, HomeAuthGuard) {
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
     * Register routes for this handler.
     */
    fun Route.install()
}

/**
 * Handles this route by:
 * - Logging incoming requests and outgoing responses.
 * - Applying the optional [auth] guard before executing [block].
 *
 * Request handling is aborted if [AuthGuard.verify] does not return
 * [GuardResult.Welcome].
 *
 * Returning early from [block] immediately stops further handling.
 *
 * Use [guard] instead to apply authentication without logging.
 *
 * @param call Ktor request representation.
 * @param auth Auth guard to apply. Defaults to [NoAuthGuard].
 * @param block Request handling block.
 */
suspend fun RouteHandler.handle(call: ApplicationCall, auth: AuthGuard = NoAuthGuard, block: suspend () -> Unit) {
    val startedAt = getTimeMillis()
    call.attributes.put(ReqResLoggingKey, startedAt)
    Fancam.debug { call.stringifyHttpRequest(unhandled = false) }

    try {
        val result = auth.verify(call)
        if (result is GuardResult.GetOut) {
            Fancam.trace { "Request refused by auth guard due to: ${result.why}" }
            return
        }
    } catch (e: Throwable) {
        val method = colorizeHttpMethod(call.request.httpMethod.value)
        val uri = call.request.uri
        Fancam.error(e) { "Error on auth verify of $name at $method $uri" }
        return
    }

    block()
}

/**
 * Handles this route using the given [auth] guard.
 *
 * Request handling is aborted if [AuthGuard.verify] does not return
 * [GuardResult.Welcome].
 *
 * Unlike [handle], this does not perform request or response logging.
 *
 * @param call Ktor request representation.
 * @param auth Auth guard to apply.
 * @param block Request handling block.
 */
suspend fun RouteHandler.guard(call: ApplicationCall, auth: AuthGuard, block: suspend () -> Unit) {
    try {
        val result = auth.verify(call)
        if (result is GuardResult.GetOut) {
            Fancam.trace { "Request refused by auth guard due to: ${result.why}" }
            return
        }
    } catch (e: Throwable) {
        val method = colorizeHttpMethod(call.request.httpMethod.value)
        val uri = call.request.uri
        Fancam.error(e) { "Error on auth verify of $name at $method $uri" }
        return
    }

    block()
}
