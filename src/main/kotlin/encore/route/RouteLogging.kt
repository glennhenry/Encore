package encore.route

import encore.fancam.Fancam
import encore.fancam.INDENT
import encore.fancam.Tags
import encore.fancam.formatter.colorizeSegment
import encore.time.TimeCenter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.PipelineCall
import io.ktor.server.plugins.origin
import io.ktor.server.request.contentLength
import io.ktor.server.request.contentType
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.ApplicationSendPipeline
import io.ktor.util.AttributeKey

fun ApplicationCall.stringifyHttpRequest(unhandled: Boolean): String {
    return buildString {
        if (unhandled) {
            appendLine("----- [Unhandled HTTP Request]")
        } else {
            appendLine("----- [HTTP Request]")
        }

        val method = colorizeHttpMethod(request.httpMethod.value)
        if (!hasBody()) {
            append("$INDENT $method ${request.uri}")
            return@buildString
        }

        appendLine("$INDENT $method ${request.uri}")

        val type = request.contentType()
        val length = request.contentLength() ?: "N/A"
        val host = request.origin.remoteHost.takeIf { it.isNotBlank() } ?: "N/A"

        append("$INDENT type=$type, length=$length, remote=$host")
    }
}

fun Application.interceptResponse() {
    sendPipeline.intercept(ApplicationSendPipeline.After) {
        val call = context.request.call
        val startedAt = call.attributes.getOrNull(ReqResLoggingKey) ?: return@intercept
        val elapsed = TimeCenter.system.elapsedTimeSince(startedAt)

        Fancam.debug(Tags.Api) { call.stringifyHttpResponse(subject, elapsed) }
    }
}

fun PipelineCall.stringifyHttpResponse(subject: Any, elapsed: Long): String {
    return buildString {
        val status = colorizeStatusCode(response.status()?.value ?: 0)
        val method = colorizeHttpMethod(request.httpMethod.value)

        appendLine("----- [HTTP Response]")
        appendLine("$INDENT $status $method ${request.uri} (${elapsed}ms)")
        append("$INDENT body: ${subject.toLogBody()}")
    }
}

val ReqResLoggingKey = AttributeKey<Long>("started-at")

fun ApplicationCall.hasBody(): Boolean {
    return request.httpMethod.value == "POST" || request.httpMethod.value == "PUT"
}

fun Any?.toLogBody(): String {
    return when (this) {
        null -> "null"
        is String -> {
            "text(length=$length)"
        }

        is ByteArray -> {
            "binary(length=$size)"
        }

        else -> {
            val type = this::class.simpleName ?: "unknown"
            "$type(${toString().length} chars)"
        }
    }
}

fun colorizeHttpMethod(method: String): String {
    return when (method.lowercase()) {
        "get" -> colorizeSegment(28, method)
        "post" -> colorizeSegment(68, method)
        "put" -> colorizeSegment(140, method)
        "delete" -> colorizeSegment(161, method)
        else -> colorizeSegment(5, method)
    }
}

fun colorizeStatusCode(status: Int): String {
    return when (status) {
        in 100..199 -> {
            colorizeSegment(249, status.toString())
        }

        in 200..299 -> {
            colorizeSegment(28, status.toString())
        }

        in 300..399 -> {
            colorizeSegment(110, status.toString())
        }

        in 400..499 -> {
            colorizeSegment(124, status.toString())
        }

        in 500..599 -> {
            colorizeSegment(124, status.toString())
        }

        else -> {
            colorizeSegment(249, "no-status")
        }
    }
}
