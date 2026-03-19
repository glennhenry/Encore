package encore.utils.logging

const val LOG_FILE_EXTENSION = "log"
const val TRACK_FILE_EXTENSION = "jsonl"
const val LOG_FILE_DIRECTORY = ".logs"

/**
 * Blank spaces to include in the log message when logging with
 * multiple lines and wants the next line to align with the upper log call.
 */
const val LOG_INDENT_PREFIX = "                                             "

/**
 * A globally accessible facade for [Fancam].
 *
 * Must be initialized before usage through [initialize] passing
 * a [FancamTemplate] implementation.
 *
 * Default implementation: [OfficialFancam].
 */
object Fancam {
    private var initialized = false
    private lateinit var fancam: FancamTemplate

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
