package encore.utils.logging

import io.ktor.util.date.getTimeMillis

class LogEventBuilder(
    private val level: Level,
    private val tag: String,
    private val src: TraceElement?,
    private val onLogCalled: (LogEvent) -> Unit
) {
    private var message: () -> String = { "Nothing was logged" }
    private var target: String? = null

    fun message(message: () -> String): LogEventBuilder = apply {
        this.message = message
    }

    fun toFile(filename: String): LogEventBuilder = apply {
        this.target = filename
    }

    fun log(full: Boolean = false) {
        onLogCalled(
            LogEvent(
                message = message,
                timestamp = getTimeMillis(),
                level = level,
                tag = tag,
                logFull = full,
                source = src,
                targetFile = target,
            )
        )
    }
}
