package encore.api.routes

import encore.context.ServerContext
import encore.fancam.Fancam
import encore.utils.UUID
import encore.ws.WsMessage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.date.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Backstage routes (devtools). In the backstage, there are three sections of tools:
 *  - **Console**: live log from server.
 *  - **Monitor**: server status and activity from server (e.g., active players, server state, running task).
 *  - **Command**: external input to control server runtime.
 *
 * Authentication/usage flow:
 * 1. User go to `/backstage`.
 * 2. Server respond with `wall.html`, asking for token.
 * 3. User type `token` in terminal to obtain an ephemeral token (1 minute).
 * 4. Server validates the token from user (via query parameters).
 *     - If invalid, send back `wall.html` (back to step 2).
 *     - If valid, set cookie of `backstage-clientId` with a new generated token (valid for 6 hours).
 *     Then, server responds with `backstage.html`.
 * 5. Client-side connects to websocket, including the previous token to the query parameter of the WS link.
 * 6. Server verify the WS query param.
 *     - If invalid, refuse the connection. This will prevent arbitrary websocket connection.
 *     The page will still be valid, though client can't do anything.
 *     - If valid, then websocket connection is approved.
 *     - From now on, refreshing page does not prompt `wall.html` anymore since server
 *     will also check this cookie.
 * 7. Client and server starts exchanging WS messages for console and commands tool.
 *     - Server sends log message on server to client for console.
 *     - Client periodically make API request to `/server-status`, including the cookie token
 *     to get monitoring status.
 *         - If cookie token is invalid, server return error status.
 *         - If cookie token is valid, server return JSON for server status.
 *     - Client can type command and it will be executed in the server.
 * 8. When session exceeded 6 hours, user needs to refresh. (step 6A).
 */
fun Route.backstageRoutes(serverContext: ServerContext, tokenStorage: MutableMap<String, Long>) {
    get("/backstage") {
        val wallHtml = File("backstage/wall.html")
        val mainHtml = File("backstage/main.html")

        // skip on developmentMode
        if (application.developmentMode) {
            Fancam.info { "Request to /backstage (passed): auth skipped in development mode" }
            call.respondFile(mainHtml)
            return@get
        }

        val token = call.request.queryParameters["token"]
        val cookie = call.request.cookies["backstage-clientId"]

        // PASS: user with cookie already authenticated before
        if (cookie != null && serverContext.sessionManager.verify(cookie)) {
            Fancam.info { "Request to /backstage (passed): user has cookie and is valid" }
            call.respondFile(mainHtml)
            return@get
        }

        // WALL: user with cookie but expired
        if (cookie != null && !serverContext.sessionManager.verify(cookie)) {
            Fancam.info { "Request to /backstage (wall): user has cookie but expired" }
            call.respondText(insertHtmlTemplate(wallHtml, "{{MESSAGE}}", "Cookie expired"), ContentType.Text.Html)
            return@get
        }

        // WALL: user without cookie and without token.
        if (token == null) {
            Fancam.info { "Request to /backstage (wall): no token provided" }
            call.respondText(insertHtmlTemplate(wallHtml, "{{MESSAGE}}", "Insert token"), ContentType.Text.Html)
            return@get
        }

        // WALL: user has unknown token
        if (!tokenStorage.contains(token)) {
            Fancam.info { "Request to /backstage (wall): got unknown token: $token" }
            call.respondText(insertHtmlTemplate(wallHtml, "{{MESSAGE}}", "Unknown token"), ContentType.Text.Html)
            return@get
        }

        // WALL: user has known token, but expired
        if (tokenStorage.contains(token) && !timeUnderMinutes(tokenStorage[token]!!, 1)) {
            Fancam.info { "Request to /backstage (wall): token already expired" }
            call.respondText(
                insertHtmlTemplate(wallHtml, "{{MESSAGE}}", "Token expired"),
                ContentType.Text.Html
            )
            return@get
        }

        // PASS: user has valid token
        val session = serverContext.sessionManager.create(
            userId = UUID.new(), validFor = 6.hours, lifetime = 6.hours
        )
        call.response.cookies.append("backstage-clientId", session.token, maxAge = 21600, path = "/backstage")
        Fancam.info { "Request to /backstage (passed): token correct and user is now logged in" }
        call.respondFile(mainHtml)
    }

    get("/backstage/server-status") {
        if (!call.ensureSession { serverContext.sessionManager.verify(it) }) return@get

        call.respond("Status received (work in progress).")
    }

    get("/backstage/cmd-help-text") {
        if (!call.ensureSession { serverContext.sessionManager.verify(it) }) return@get

        val commands = serverContext.commandDispatcher.getAllRegisteredCommands()
        val html = StringBuilder()

        html.append("<ul>")

        for (cmd in commands) {
            html.append("<li><b><code>${cmd.commandId}</code></b>: ${cmd.description}")
            html.append("<ol>")

            for (variant in cmd.variants) {
                html.append("<li>")
                html.append("<ul>")

                // Signature list
                for (sig in variant.signature) {
                    html.append("<li><code>${sig.id}</code> (<code>${sig.expectedType}</code>): ${sig.description}</li>")
                }

                html.append("</ul>")
                html.append("</li>")
            }

            html.append("</ol>")
            html.append("</li>")
        }

        html.append("</ul>")

        call.respondText(html.toString(), ContentType.Text.Html)
    }

    webSocket("/backstage/ws") {
        val token = if (application.developmentMode) {
            // dev mode uses arbitrary identifier
            "DEV-${getTimeMillis()}"
        } else {
            // websocket can't send cookie, token cookie for WS is included in the param instead
            // also verify the token
            call.request.queryParameters["token"]
                ?.takeIf { serverContext.sessionManager.verify(it) }
        }

        if (token == null) {
            Fancam.info { "WebSocket request for /backstage: failed with invalid token=$token" }
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
            return@webSocket
        }
        Fancam.info { "WebSocket request for /backstage: success with $token" }

        serverContext.wsManager.addClient(token, this)

        try {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val msg = frame.readText()
                    try {
                        val wsMessage = Json.decodeFromString<WsMessage>(msg)
                        if (wsMessage.type == "close") {
                            serverContext.wsManager.removeClient(token)
                            break
                        }
                        serverContext.wsManager.handleMessage(this, wsMessage)
                    } catch (e: Exception) {
                        Fancam.error { "Failed to parse WS message: $msg\n$e" }
                    }
                }
            }
        } catch (e: Exception) {
            Fancam.error { "Error in websocket for client $this: $e" }
        } finally {
            serverContext.wsManager.removeClient(token)
            Fancam.info { "Client $this disconnected from websocket" }
        }
    }
}

fun timeUnderMinutes(timeMillis: Long, minutes: Int): Boolean {
    return getTimeMillis() - timeMillis < minutes.minutes.inWholeMilliseconds
}

fun insertHtmlTemplate(file: File, templateId: String, message: String): String {
    return file.readText().replace(templateId, message)
}

suspend fun ApplicationCall.ensureSession(verify: (String) -> Boolean): Boolean {
    val cookie = request.cookies["backstage-clientId"]
    val cookieValid = cookie != null && verify(cookie)

    if (!application.developmentMode && !cookieValid) {
        respond(HttpStatusCode.Forbidden, "Session invalid, please re-login")
        return false
    }

    return true
}
