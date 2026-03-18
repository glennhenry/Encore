package encore.utils.logging

import encore.startup.venue.EncoreFancamConfig

/**
 * Represent application logging interface.
 *
 * Implementations of this interface allow logging within application components
 * to be substituted with any desired backend (e.g., normal logger, suppressed logger,
 * test logger) via dependency injection.
 *
 * Injecting [ILogger] should only be done only when a class explicitly needs
 * logger substitution (e.g., for testing). For general logging, always use the
 * [Logger] object to avoid unnecessary boilerplate.
 *
 * Each log level (verbose, debug, info, warn, error) supports:
 *  - Direct string logging
 *  - Lazy logging via lambda (`msg: () -> String`) to avoid unnecessary
 *    string construction when the log level is disabled
 *  - Optional routing to multiple [LogTarget] destinations
 *
 * @param tag A short context label, such as a subsystem, class name, or module.
 * @param msg The log message, either directly or lazily via lambda.
 * @param logFull Whether the log should be fully printed.
 */
interface FancamTemplate {
    val config: EncoreFancamConfig
    val sourceResolver: FancamSourceResolver

    fun trace(tag: String = "", msg: () -> String)
    fun debug(tag: String = "", msg: () -> String)
    fun info(tag: String = "", msg: () -> String)
    fun warn(tag: String = "", msg: () -> String)
    fun error(tag: String = "", msg: () -> String)

    fun event(level: Level, tag: String = ""): LogEventBuilder
    fun track(name: String): TrackEventBuilder
}
