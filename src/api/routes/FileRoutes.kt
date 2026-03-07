package api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.io.File

fun Route.fileRoutes() {
    get("/") {
        call.respondFile(File("assets/index.html"))
    }
    staticFiles("site", File("assets/site"))

    get("/docs") {
        val docsIndex = File("docs/index.html")
        if (docsIndex.exists()) {
            call.respondFile(docsIndex)
        } else {
            call.respond(HttpStatusCode.NotFound, "Only available in production; Please start the docs using vite server or build the server first.")
        }
    }

    staticFiles("docs", File("docs"))
}
