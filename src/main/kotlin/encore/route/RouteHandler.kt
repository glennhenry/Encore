package encore.route

import encore.fancam.Fancam
import encore.fancam.Tags
import encore.route.guard.AuthGuard
import encore.route.guard.GuardResult
import encore.route.guard.NoAuthGuard
import encore.time.TimeCenter
import encore.utils.support.className
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

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
     * Register routes for this handler.
     */
    fun Route.install()
}

/**
 * Utility to handle an endpoint route which:
 * - Logs incoming requests and outgoing responses.
 * - Applies the optional [auth] guard before executing [block].
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
suspend fun RoutingContext.handle(call: ApplicationCall, auth: AuthGuard = NoAuthGuard, block: suspend () -> Unit) {
    val startedAt = TimeCenter.now()
    call.attributes.put(ReqResLoggingKey, startedAt)

    val req = call.stringifyHttpRequest(unhandled = false)
    Fancam.debug(Tags.Api) { req }

    try {
        val result = auth.verify(call)
        if (result is GuardResult.GetOut) {
            Fancam.trace(Tags.Api) { "Request refused by auth guard due to: ${result.why}" }
            return
        }
    } catch (e: Throwable) {
        val method = colorizeHttpMethod(call.request.httpMethod.value)
        val uri = call.request.uri
        Fancam.error(e, Tags.Api) { "Scandal on auth verify of ${className()} at $method $uri" }
        return
    }

    block()
}

/**
 * Utility to handle an endpoint route which apply the given [auth] guard.
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
suspend fun RoutingContext.guard(call: ApplicationCall, auth: AuthGuard, block: suspend () -> Unit) {
    try {
        val result = auth.verify(call)
        if (result is GuardResult.GetOut) {
            Fancam.trace(Tags.Api) { "Request refused by auth guard due to: ${result.why}" }
            return
        }
    } catch (e: Throwable) {
        val method = colorizeHttpMethod(call.request.httpMethod.value)
        val uri = call.request.uri
        Fancam.error(e, Tags.Api) { "Scandal on auth verify of ${className()} at $method $uri" }
        return
    }

    block()
}
