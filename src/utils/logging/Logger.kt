package utils.logging

import io.ktor.util.date.*
import utils.constants.AnsiColors
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

/**
 * The default logger implementation of [ILogger].
 *
 * ## Overview
 *
 * `Logger` supports logging with multiple levels, custom tags, configurable targets,
 * and optional truncation. It is self-contained and file-rotation capable,
 * with no external dependencies.
 *
 * ## Usage
 *
 * ### 1. Basic Logging
 * Log simple messages at any level with custom tag:
 * ```kotlin
 * const val ClassTag = "TagForSomeClass"
 * const val GlobalTag = "AGlobalTag"
 *
 * Logger.debug("TAG", "Debug message")
 * Logger.info(ClassTag, "Informational log")
 * Logger.error(GlobalTag, "Something went wrong")
 * ```
 *
 * ### 2. Lazy Evaluation
 * Defer expensive message construction — the lambda is only evaluated if the level is active:
 * ```kotlin
 * Logger.debug { "Computed value: ${'$'}{expensiveOperation()}" }
 * ```
 *
 * ### 3. Structured Logging with Presets
 * Use predefined configurations for log target:
 * ```kotlin
 * Logger.error(targets = SocketError) { "Socket failed to connect, log to socket-error.log" }
 * Logger.info(targets = SendToClient) { "Message sent to client" }
 * ```
 *
 * ### 4. Override Truncation
 * By default, messages longer than 500 characters are truncated. You can override this:
 * ```kotlin
 * Logger.info(logFull = false) { "Full JSON payload..." }
 * ```
 *
 * ## Features
 * - **Log levels:** (0) [LogLevel.Verbose], [LogLevel.Debug], [LogLevel.Info], [LogLevel.Warn], [LogLevel.Error], [LogLevel.Nothing] (6)
 * - **Custom Tag:** Used to logically group log messages or provide extra context.
 *                   Typically, a class, related piece of code, functions, define
 *                   some tag constants to be used thorough all logging call in the code.
 * - **Output targets:**
 *     - [LogTarget.Print] = Standard output
 *     - [LogTarget.File] = Log file (with automatic rotation)
 *     - [LogTarget.Client] = Send to connected client (currently disabled)
 * - **Formatting:**
 *     - Timestamped entries (`yyyy-MM-dd HH:mm:ss.SSS`)
 *     - Optional ANSI color output
 *     - Truncation for long messages
 *
 * ## Configuration
 * - Set via `updateSettings()` with `LoggerSettings`.
 * - Create new presets:
 *   ```kotlin
 *   val CustomAPIError = setOf(LogTarget.Print, LogTarget.File(LogFile.APIServerError))
 *   Logger.error(CustomAPIError) { "Custom API handler failure" }
 *   ```
 * - Extend file destinations:
 *   ```kotlin
 *   private val logFileMap = mapOf(
 *       LogFile.CustomFile to File(".logs/custom_file-1.log"),
 *   ).also { File(".logs").mkdirs() }
 *   ```
 */
object Logger : ILogger {
    private val logFileMap = mapOf(
        LogFile.ClientError to File(".logs/client_error-1.log"),
        LogFile.AssetsError to File(".logs/assets_error-1.log"),
        LogFile.APIServerError to File(".logs/api_server_error-1.log"),
        LogFile.SocketServerError to File(".logs/socket_server_error-1.log"),
        LogFile.DatabaseError to File(".logs/database_error-1.log"),
    ).also { File(".logs").mkdirs() }

    private var settings = LoggerSettings()

    // blocking queue of log calls processed by a separate thread
    // ensuring proper log call ordering while not blocking main thread
    private val logQueue = LinkedBlockingQueue<LogCall>()
    private val executor = Executors.newSingleThreadExecutor()

    override fun verbose(tag: String, msg: String, logFull: Boolean) = verbose(tag, logFull) { msg }
    override fun verbose(tag: String, logFull: Boolean, msg: () -> String) = verbose(tag, logFull, Default, msg)
    override fun verbose(tag: String, logFull: Boolean, targets: Set<LogTarget>, msg: () -> String) =
        log(tag, logFull, targets, LogLevel.Verbose, msg)

    override fun debug(tag: String, msg: String, logFull: Boolean) = debug(tag, logFull) { msg }
    override fun debug(tag: String, logFull: Boolean, msg: () -> String) = debug(tag, logFull, Default, msg)
    override fun debug(tag: String, logFull: Boolean, targets: Set<LogTarget>, msg: () -> String) =
        log(tag, logFull, targets, LogLevel.Debug, msg)

    override fun info(tag: String, msg: String, logFull: Boolean) = info(tag, logFull) { msg }
    override fun info(tag: String, logFull: Boolean, msg: () -> String) = info(tag, logFull, Default, msg)
    override fun info(tag: String, logFull: Boolean, targets: Set<LogTarget>, msg: () -> String) =
        log(tag, logFull, targets, LogLevel.Info, msg)

    override fun warn(tag: String, msg: String, logFull: Boolean) = warn(tag, logFull) { msg }
    override fun warn(tag: String, logFull: Boolean, msg: () -> String) = warn(tag, logFull, Default, msg)
    override fun warn(tag: String, logFull: Boolean, targets: Set<LogTarget>, msg: () -> String) =
        log(tag, logFull, targets, LogLevel.Warn, msg)

    override fun error(tag: String, msg: String, logFull: Boolean) = error(tag, logFull) { msg }
    override fun error(tag: String, logFull: Boolean, msg: () -> String) = error(tag, logFull, Default, msg)
    override fun error(tag: String, logFull: Boolean, targets: Set<LogTarget>, msg: () -> String) =
        log(tag, logFull, targets, LogLevel.Error, msg)

    private fun log(
        tag: String,
        logFull: Boolean,
        targets: Set<LogTarget>,
        level: LogLevel,
        msg: () -> String,
    ) {
        if (level < settings.minimumLevel) return

        val rawMsg = msg()
            .lineSequence()
            .joinToString("\n") { line ->
                if (!logFull && line.length > settings.maximumLogMessageLineLength) {
                    line.take(settings.maximumLogMessageLineLength) + " <...truncated>"
                } else {
                    line
                }
            }

        logQueue.offer(LogCall(tag, level, targets, buildSourceHint(), rawMsg))
    }

    init {
        executor.execute {
            while (true) {
                val call = logQueue.take()
                call.targets.forEach { target ->
                    val now = getTimeMillis()
                    when (target) {
                        LogTarget.Print -> {
                            val timestamp = settings.logDateFormatter.format(now)
                            val levelLabel = call.level.label()

                            if (settings.colorfulLog) {
                                val coloredMessage = if (settings.colorizeLevelLabelOnly) {
                                    buildLogMessage(
                                        timestamp = "[$timestamp]",
                                        source = call.source,
                                        level = colorizeText(call.level, "[$levelLabel]"),
                                        tag = call.tag,
                                        rawMsg = call.rawMsg
                                    )
                                } else {
                                    colorizeText(
                                        call.level, buildLogMessage(
                                            timestamp = "[$timestamp]",
                                            source = call.source,
                                            level = "[$levelLabel]",
                                            tag = call.tag,
                                            rawMsg = call.rawMsg
                                        )
                                    )
                                }

                                BypassJansi.println(coloredMessage)
                            } else {
                                BypassJansi.println(
                                    buildLogMessage(
                                        timestamp = "[$timestamp]",
                                        source = call.source,
                                        level = "[$levelLabel]",
                                        tag = call.tag,
                                        rawMsg = call.rawMsg
                                    )
                                )
                            }
                        }

                        is LogTarget.File -> {
                            val message = buildLogMessage(
                                timestamp = "[${settings.logDateFormatter.format(now)}]",
                                source = call.source,
                                level = "[${call.level.label()}]",
                                tag = call.tag,
                                rawMsg = call.rawMsg
                            )
                            writeToFile(target.file, message)
                        }

                        LogTarget.Client -> {
                            // currently disabled
                            // REPLACE
                        }
                    }
                }
            }
        }
    }

    /**
     * Find source file and line number of the log call.
     */
    private fun buildSourceHint(): String {
        val caller = Thread.currentThread().stackTrace
            .dropWhile { element -> LOGGER_CALL_WHITELIST.contains(element.className) }
            .firstOrNull() ?: return "(UnknownSource)"

        val file = caller.fileName
        val line = caller.lineNumber

        return formatFileName(file, line)
    }

    private fun formatFileName(file: String?, line: Int): String {
        if (file == null) return "[Unknown filename]"

        if (file.length > settings.fileNamePadding) {
            val truncated = file.take(settings.fileNamePadding - 2) + "..."
            return "($truncated)"
        } else {
            val padded = file.padStart(settings.fileNamePadding - line.toString().length, ' ')
            return "($padded:$line)"
        }
    }

    /**
     * Build log message in the order: 'timestamp source level <tag> rawMsg'.
     */
    private fun buildLogMessage(timestamp: String, source: String, tag: String, level: String, rawMsg: String): String {
        val boldedLevel = AnsiColors.bold(level)
        return if (tag.isEmpty()) {
            "$timestamp$source$boldedLevel $rawMsg"
        } else {
            "$timestamp$source$boldedLevel <$tag> $rawMsg"
        }
    }

    /**
     * Colorize certain text message using ANSI colors.
     */
    private fun colorizeText(level: LogLevel, text: String): String {
        val fg: String
        val bg: String

        if (settings.useForegroundColor) {
            bg = ""
            fg = when (level) {
                LogLevel.Verbose -> AnsiColors.VerboseFg
                LogLevel.Debug -> AnsiColors.DebugFg
                LogLevel.Info -> AnsiColors.InfoFg
                LogLevel.Warn -> AnsiColors.WarnFg
                LogLevel.Error -> AnsiColors.ErrorFg
                LogLevel.Nothing -> ""
            }
        } else {
            fg = AnsiColors.BlackText
            bg = when (level) {
                LogLevel.Verbose -> AnsiColors.VerboseBg
                LogLevel.Debug -> AnsiColors.DebugBg
                LogLevel.Info -> AnsiColors.InfoBg
                LogLevel.Warn -> AnsiColors.WarnBg
                LogLevel.Error -> AnsiColors.ErrorBg
                LogLevel.Nothing -> ""
            }
        }

        return "$bg$fg$text${AnsiColors.Reset}"
    }

    /**
     * Set new settings of logger.
     *
     * @param original The current setting to be updated.
     */
    fun updateSettings(original: (LoggerSettings) -> LoggerSettings) {
        this.settings = original(this.settings)
    }

    private fun writeToFile(file: LogFile, message: String) {
        logFileMap[file]?.let { targetFile ->
            if (targetFile.exists() && targetFile.length() > settings.maximumLogFileSize) {
                rotateLogFile(targetFile)
            }
            targetFile.appendText("$message\n")
        }
    }

    private fun rotateLogFile(file: File): File {
        val match = Regex("""(.+)-(\d+)\.log""").matchEntire(file.name) ?: return file
        val (baseName, currentIndexStr) = match.destructured
        val nextIndex = (currentIndexStr.toInt() % settings.maximumLogFileRotation) + 1
        val newFile = File(file.parentFile, "$baseName-$nextIndex.log")
        if (newFile.exists()) newFile.delete()
        return newFile
    }

    /**
     * If you use `appendLine` when building log message,
     * you will want the new line to be aligned.
     *
     * Use this constant indent string to ensure new lines in log message
     * are aligned with each new log call.
     */
    const val LOG_INDENT_PREFIX = "                                             "
}

/**
 * Contains stack trace to ignore, usually from log source file itself,
 * thread, executor, or any logging utilities.
 */
val LOGGER_CALL_WHITELIST = listOf(
    "utils.logging.Logger", "utils.logging.ILogger", "java.lang.Thread", "ThreadPoolExecutor",
)

/**
 * Raw console access that bypass Jansi.
 *
 * This is only needed when you want to style the console (e.g., colored text, emoji display)
 */
object BypassJansi {
    private val rawOut = PrintStream(FileOutputStream(FileDescriptor.out), true, Charsets.UTF_8)
    fun println(msg: String) = rawOut.println(msg)
}

/**
 * @property fileNamePadding The amount of blank space to align filenames.
 *                           Filename exceeding this number won't be aligned.
 */
data class LoggerSettings(
    val minimumLevel: LogLevel = LogLevel.Verbose,
    val colorfulLog: Boolean = true,
    val colorizeLevelLabelOnly: Boolean = true,
    val useForegroundColor: Boolean = false,
    val fileNamePadding: Int = 25,
    val tagPadding: Int = 20,
    val maximumLogMessageLineLength: Int = 500,
    val maximumLogFileSize: Int = 5 * 1024 * 1024,
    val maximumLogFileRotation: Int = 5,
    val logDateFormatter: SimpleDateFormat = SimpleDateFormat("HH:mm:ss.SSS"),
    val fileDateFormatter: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"),
)

enum class LogLevel { Verbose, Debug, Info, Warn, Error, Nothing }

fun Int.toLogLevel(): LogLevel {
    return when (this) {
        0 -> LogLevel.Verbose
        1 -> LogLevel.Debug
        2 -> LogLevel.Info
        3 -> LogLevel.Warn
        4 -> LogLevel.Error
        5 -> LogLevel.Nothing
        else -> LogLevel.Verbose
    }
}

fun LogLevel.toInt(): Int {
    return when (this) {
        LogLevel.Verbose -> 0
        LogLevel.Debug -> 1
        LogLevel.Info -> 2
        LogLevel.Warn -> 3
        LogLevel.Error -> 4
        LogLevel.Nothing -> 5
    }
}

fun LogLevel.label(): String {
    return when (this) {
        LogLevel.Verbose -> "V"
        LogLevel.Debug -> "D"
        LogLevel.Info -> "I"
        LogLevel.Warn -> "W"
        LogLevel.Error -> "E"
        LogLevel.Nothing -> "N"
    }
}

sealed class LogTarget {
    object Print : LogTarget()
    object Client : LogTarget()
    data class File(val file: LogFile) : LogTarget()
}

enum class LogFile { ClientError, AssetsError, APIServerError, SocketServerError, DatabaseError }

data class LogCall(
    val tag: String,
    val level: LogLevel,
    val targets: Set<LogTarget>,
    val source: String,
    val rawMsg: String,
)

val Default = setOf(LogTarget.Print)
val SocketError = setOf(LogTarget.Print, LogTarget.File(LogFile.SocketServerError))
val DbError = setOf(LogTarget.Print, LogTarget.File(LogFile.DatabaseError))
val ApiError = setOf(LogTarget.Print, LogTarget.File(LogFile.APIServerError))
val ClientError = setOf(LogTarget.Print, LogTarget.File(LogFile.ClientError))
val AssetsError = setOf(LogTarget.Print, LogTarget.File(LogFile.AssetsError), LogTarget.File(LogFile.ClientError))
val SendToClient = setOf(LogTarget.Print, LogTarget.Client)
