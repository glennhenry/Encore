package utils.logging

/**
 * Empty logger (no-operation).
 */
class EmptyLogger: ILogger {
    override fun verbose(tag: String, msg: String, logFull: Boolean) {}
    override fun verbose(tag: String, logFull: Boolean, msg: () -> String) {}
    override fun verbose(tag: String, logFull: Boolean, targets: Set<LogTarget>, msg: () -> String) {}

    override fun debug(tag: String, msg: String, logFull: Boolean) {}
    override fun debug(tag: String, logFull: Boolean, msg: () -> String) {}
    override fun debug(tag: String, logFull: Boolean, targets: Set<LogTarget>, msg: () -> String) {}

    override fun info(tag: String, msg: String, logFull: Boolean) {}
    override fun info(tag: String, logFull: Boolean, msg: () -> String) {}
    override fun info(tag: String, logFull: Boolean, targets: Set<LogTarget>, msg: () -> String) {}

    override fun warn(tag: String, msg: String, logFull: Boolean) {}
    override fun warn(tag: String, logFull: Boolean, msg: () -> String) {}
    override fun warn(tag: String, logFull: Boolean, targets: Set<LogTarget>, msg: () -> String) {}

    override fun error(tag: String, msg: String, logFull: Boolean) {}
    override fun error(tag: String, logFull: Boolean, msg: () -> String) {}
    override fun error(tag: String, logFull: Boolean, targets: Set<LogTarget>, msg: () -> String) {}
}
