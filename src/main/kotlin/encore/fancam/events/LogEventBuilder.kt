package encore.fancam.events

import encore.fancam.Fancam
import io.ktor.util.date.getTimeMillis

/**
 * A DSL-style builder used to construct [LogEvent].
 *
 * Example (via [Fancam]):
 * ```kotlin
 * Fancam.event(Level.Info, "InventoryService")
 *       .message { "service working..." }
 *       .toFile("InventoryService")
 *       .log(full = true)
 * ```
 *
 * @param onLogCalled Callback used when this builder is finalized, providing
 *                    the constructed [LogEvent]. The provided callback is expected
 *                    to output the log event to console.
 */
class LogEventBuilder(
    private val level: Level,
    private val tag: String,
    private val src: TraceElement?,
    private val onLogCalled: (LogEvent) -> Unit
) {
    private var message: () -> String = { "Nothing was logged" }
    private var target: String? = null

    /**
     * Set the lazily evaluated message for the log event.
     */
    fun message(message: () -> String): LogEventBuilder = apply {
        this.message = message
    }

    /**
     * Add file as an output target with the specified [filename].
     *
     * This method sets the internal file target, subsequent call to this
     * will overwrites the previous.
     */
    fun toFile(filename: String): LogEventBuilder = apply {
        this.target = filename
    }

    /**
     * Finalize the builder by calling the [onLogCalled] callback.
     *
     * @param full To specify whether the log event should be printed fully.
     */
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
