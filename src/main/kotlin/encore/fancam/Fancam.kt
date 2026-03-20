package encore.fancam

import encore.fancam.events.Level
import encore.fancam.events.LogEvent
import encore.fancam.events.TrackEvent
import encore.fancam.events.LogEventBuilder
import encore.fancam.events.TrackEventBuilder
import encore.fancam.impl.FancamTemplate
import encore.fancam.impl.OfficialFancam
import encore.fancam.impl.RehearsalFancam

const val LOG_FILE_EXTENSION = "log"
const val TRACK_FILE_EXTENSION = "jsonl"
const val LOG_FILE_DIRECTORY = ".logs"

/**
 * A globally accessible facade for [Fancam].
 *
 * Provides a static entry point for logging and tracking events
 * through a [FancamTemplate] implementation.
 *
 * **Initialization:** Should be initialized before use via [initialize]
 * by passing a concrete [FancamTemplate].
 *
 * **Default behavior:** By default, the implementation is [RehearsalFancam],
 * which is a simple, no-dependency version typically used for testing
 * or early bootstrap. This default can later be upgraded to a full-featured
 * implementation.
 *
 * **Recommended implementation:** [OfficialFancam].
 */
object Fancam {
    private var initialized = false
    // to avoid annoying must init; also for default in tests
    private var fancam: FancamTemplate = RehearsalFancam()

    /**
     * Initialize the global [Fancam] instance.
     *
     * @param fancam The [FancamTemplate] implementation to use.
     */
    fun initialize(fancam: FancamTemplate) {
        if (initialized) {
            warn { "Fancam is already initialized." }
            return
        }
        this.fancam = fancam
    }

    /**
     * Log with the [Level.Trace], category [tag], and lazy message of [msg].
     */
    fun trace(tag: String = "", msg: () -> String) = fancam.trace(tag, msg)

    /**
     * Log with the [Level.Debug], category [tag], and lazy message of [msg].
     */
    fun debug(tag: String = "", msg: () -> String) = fancam.debug(tag, msg)

    /**
     * Log with the [Level.Info], category [tag], and lazy message of [msg].
     */
    fun info(tag: String = "", msg: () -> String) = fancam.info(tag, msg)

    /**
     * Log with the [Level.Warn], category [tag], and lazy message of [msg].
     */
    fun warn(tag: String = "", msg: () -> String) = fancam.warn(tag, msg)

    /**
     * Log with the [Level.Error], category [tag], and lazy message of [msg].
     */
    fun error(tag: String = "", msg: () -> String) = fancam.error(tag, msg)

    /**
     * Create a logging event for [LogEvent] with the [LogEventBuilder].
     */
    fun event(level: Level, tag: String = ""): LogEventBuilder = fancam.event(level, tag)

    /**
     * Create a structured logging event for [TrackEvent] with the [TrackEventBuilder].
     */
    fun track(name: String): TrackEventBuilder = fancam.track(name)
}

/**
 * Blank spaces to include in the log message when logging with
 * multiple lines and wants the next line to align with the upper log call.
 */
const val LOG_INDENT_PREFIX = "                                             "
