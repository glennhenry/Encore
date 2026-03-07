package utils.logging

/**
 * A logger implementation intended for testing. It prints log message via [Logger] object,
 * record the most recent messages, and provides access to them for assertions.
 *
 * All additional logging parameters (such as `logFull`, `tag`, or `targets`)
 * are ignored. Only the final rendered message string is stored.
 *
 * Logged messages can be retrieved using methods such as
 * [getLastVerboseCalls], [getLastDebugCalls], and others.
 */
class TestLogger : ILogger {
    private val v = mutableListOf<String>()
    private val d = mutableListOf<String>()
    private val i = mutableListOf<String>()
    private val w = mutableListOf<String>()
    private val e = mutableListOf<String>()

    override fun verbose(tag: String, msg: String, logFull: Boolean) { Logger.verbose(tag, msg, logFull).also { v.add(msg) } }
    override fun verbose(tag: String, logFull: Boolean, msg: () -> String) { Logger.verbose(tag, logFull, msg).also { v.add(msg()) } }
    override fun verbose(tag: String, logFull: Boolean, targets: Set<LogTarget>, msg: () -> String) { Logger.verbose(tag, logFull, targets, msg).also { v.add(msg()) } }

    override fun debug(tag: String, msg: String, logFull: Boolean) { Logger.debug(tag, msg, logFull).also { d.add(msg) } }
    override fun debug(tag: String, logFull: Boolean, msg: () -> String) { Logger.debug(tag, logFull, msg).also { d.add(msg()) } }
    override fun debug(tag: String, logFull: Boolean, targets: Set<LogTarget>, msg: () -> String) { Logger.debug(tag, logFull, targets, msg).also { d.add(msg()) } }

    override fun info(tag: String, msg: String, logFull: Boolean) { Logger.info(tag, msg, logFull).also { i.add(msg) } }
    override fun info(tag: String, logFull: Boolean, msg: () -> String) { Logger.info(tag, logFull, msg).also { i.add(msg()) } }
    override fun info(tag: String, logFull: Boolean, targets: Set<LogTarget>, msg: () -> String) { Logger.info(tag, logFull, targets, msg).also { i.add(msg()) } }

    override fun warn(tag: String, msg: String, logFull: Boolean) { Logger.warn(tag, msg, logFull).also { w.add(msg) } }
    override fun warn(tag: String, logFull: Boolean, msg: () -> String) { Logger.warn(tag, logFull, msg).also { w.add(msg()) } }
    override fun warn(tag: String, logFull: Boolean, targets: Set<LogTarget>, msg: () -> String) { Logger.warn(tag, logFull, targets, msg).also { w.add(msg()) } }

    override fun error(tag: String, msg: String, logFull: Boolean) { Logger.error(tag, msg, logFull).also { e.add(msg) } }
    override fun error(tag: String, logFull: Boolean, msg: () -> String) { Logger.error(tag, logFull, msg).also { e.add(msg()) } }
    override fun error(tag: String, logFull: Boolean, targets: Set<LogTarget>, msg: () -> String) { Logger.error(tag, logFull, targets, msg).also { e.add(msg()) } }

    fun getLastVerboseCalls(n: Int): List<String> = v.takeLast(n)
    fun getLastDebugCalls(n: Int): List<String> = d.takeLast(n)
    fun getLastInfoCalls(n: Int): List<String> = i.takeLast(n)
    fun getLastWarnCalls(n: Int): List<String> = w.takeLast(n)
    fun getLastErrorCalls(n: Int): List<String> = e.takeLast(n)
}
