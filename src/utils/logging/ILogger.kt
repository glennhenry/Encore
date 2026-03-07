package utils.logging

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
interface ILogger {
    fun verbose(tag: String = "", msg: String, logFull: Boolean = true)
    fun verbose(tag: String = "", logFull: Boolean = true, msg: () -> String)
    fun verbose(tag: String = "", logFull: Boolean = true, targets: Set<LogTarget>, msg: () -> String)

    fun debug(tag: String = "", msg: String, logFull: Boolean = true)
    fun debug(tag: String = "", logFull: Boolean = true, msg: () -> String)
    fun debug(tag: String = "", logFull: Boolean = true, targets: Set<LogTarget>, msg: () -> String)

    fun info(tag: String = "", msg: String, logFull: Boolean = true)
    fun info(tag: String = "", logFull: Boolean = true, msg: () -> String)
    fun info(tag: String = "", logFull: Boolean = true, targets: Set<LogTarget>, msg: () -> String)

    fun warn(tag: String = "", msg: String, logFull: Boolean = true)
    fun warn(tag: String = "", logFull: Boolean = true, msg: () -> String) = warn(tag, logFull, Default, msg)
    fun warn(tag: String = "", logFull: Boolean = true, targets: Set<LogTarget>, msg: () -> String)

    fun error(tag: String = "", msg: String, logFull: Boolean = true)
    fun error(tag: String = "", logFull: Boolean = true, msg: () -> String)
    fun error(tag: String = "", logFull: Boolean = true, targets: Set<LogTarget>, msg: () -> String)
}
