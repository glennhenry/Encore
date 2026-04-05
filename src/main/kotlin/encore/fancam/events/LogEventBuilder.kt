package encore.fancam.events

import encore.fancam.Fancam
import encore.fancam.impl.OfficialFancam
import io.ktor.util.date.getTimeMillis

/**
 * A DSL-style builder used to construct [LogEvent].
 *
 * Example (via [Fancam]):
 * ```kotlin
 * // log to console
 * Fancam.event(Level.Info, "InventorySubunit")
 *       .message { "subunit working..." }
 *       .log(full = true)
 *
 * // log to console + file
 * Fancam.event(Level.Info, "InventorySubunit")
 *       .message { "subunit working..." }
 *       .setFileTarget("Subunit")
 *       .log(full = true)
 *
 * // log to file
 * Fancam.event(Level.Info, "InventorySubunit")
 *       .message { "subunit working..." }
 *       .logToFile("Subunit")
 * ```
 *
 * @param onLogCalled Callback used when this builder is finalized, providing
 *                    the constructed [LogEvent] and boolean value specifying
 *                    whether the log is `fileOnlyOutput`.
 */
class LogEventBuilder(
    private val level: Level,
    private val tag: String,
    private val src: TraceElement?,
    private val onLogCalled: (LogEvent, Boolean) -> Unit
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
     * Set the file target of this log event.
     *
     * Only one file target is possible, subsequent call will overwrite it.
     *
     * Setting the file target is not needed if [logToFileOnly] is called at the end.
     *
     * @param filename The filename without extension.
     */
    fun setFileTarget(filename: String) {
        this.target = filename
    }

    /**
     * Finalize the builder and log the event.
     *
     * This will also output the log event into a file if [setFileTarget]
     * was called before. If you want file only output, use [logToFileOnly].
     *
     * @param full To specify whether the log event should be printed fully.
     *             This is ignored on [OfficialFancam] implementation for file target.
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
                targetFile = null,
            ), false
        )
    }

    /**
     * Finalize the builder and log the event to the specified filename by [setFileTarget].
     *
     * By using this method, log won't be outputted to console.
     *
     * Note: the file target is as specified by [filename] and not by [setFileTarget].
     *
     * @param filename The filename without extension.
     */
    fun logToFileOnly(filename: String) {
        onLogCalled(
            LogEvent(
                message = message,
                timestamp = getTimeMillis(),
                level = level,
                tag = tag,
                logFull = true,
                source = src,
                targetFile = filename,
            ), true
        )
    }
}
