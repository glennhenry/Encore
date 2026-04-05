package encore.fancam.formatter

import encore.EncoreFancamConfig
import encore.fancam.events.Level
import encore.fancam.events.LogEvent
import encore.fancam.events.TraceElement
import encore.fancam.events.label
import java.text.SimpleDateFormat
import kotlin.text.padStart
import kotlin.text.take

/**
 * Formatter implementation tailored for:
 * - Log event into console output, which is formatted with ANSI color based on
 *   [EncoreFancamConfig.colorEnabled].
 * - Log event into file output, which is never truncated, not colored, and have
 *   tags always included. This will be set from the flag [isFileTarget].
 */
class LogEventFancamFormatter(
    private val config: EncoreFancamConfig,
    private val isFileTarget: Boolean,
) : FancamFormatter<LogEvent> {
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS")

    override fun format(event: LogEvent): String {
        val timestamp = formatTimestamp(event.timestamp, timeFormat)
        val source = formatSourceHint(event.source, config.fileNamePadding, isFileTarget)
        val level = if (config.colorEnabled && !isFileTarget) {
            colorizeText(event.level, "[${event.level.label()}]")
        } else {
            "[${event.level.label()}]"
        }

        if (isFileTarget) {
            return buildString {
                // [14:21:54.221](Application.kt:22)[D][InventorySubunit] debug message
                // everything printed, no truncation, no padding
                append("[$timestamp]$source$level[${event.tag.ifBlank { "_" }}] ${event.message()}")
                if (event.level == Level.Error) {
                    appendLine()
                    appendLine(event.throwable.toLimitedString())
                }
            }
        }

        val message = event.message()
            .lineSequence()
            .joinToString("\n") { line ->
                if (!event.logFull && line.length > config.maxLineLength) {
                    line.take(config.maxLineLength) + " <...truncated>"
                } else {
                    line
                }
            }

        val tag = if (event.tag.length > config.tagPadding) {
            event.tag.take(config.tagPadding - 3) + "..."
        } else {
            event.tag.padEnd(config.tagPadding, '_')
        }

        return buildString {
            // [14:21:54.221](      Application.kt:22)[D][Server    ] debug message
            // have truncation on tag and message, padding is preserved for source and tag
            append("[$timestamp]$source$level[$tag] $message")
            if (event.level == Level.Error) {
                appendLine()
                appendLine(event.throwable.toLimitedString())
            }
        }
    }

    private fun formatTimestamp(timestamp: Long, format: SimpleDateFormat): String {
        return format.format(timestamp)
    }

    private fun formatSourceHint(source: TraceElement?, fileNamePadding: Int, noTruncation: Boolean): String {
        if (source == null) return "(UnknownSource)"
        val file = source.filename
        val line = source.lineNumber

        if (file == null) return "(UnknownFilename)"

        if (noTruncation) {
            return "($file:$line)"
        }

        if (file.length > fileNamePadding) {
            val truncated = file.take(fileNamePadding - 2) + "..."
            return "($truncated)"
        } else {
            val padded = file.padStart(fileNamePadding - line.toString().length, ' ')
            return "($padded:$line)"
        }
    }

    private fun colorizeText(level: Level, text: String): String {
        val fg: String
        val bg: String

        if (config.useBackgroundColor) {
            fg = AnsiColors.BlackText
            bg = when (level) {
                Level.Trace -> AnsiColors.TraceBg
                Level.Debug -> AnsiColors.DebugBg
                Level.Info -> AnsiColors.InfoBg
                Level.Warn -> AnsiColors.WarnBg
                Level.Error -> AnsiColors.ErrorBg
                Level.Off -> ""
            }
        } else {
            bg = ""
            fg = when (level) {
                Level.Trace -> AnsiColors.TraceFg
                Level.Debug -> AnsiColors.DebugFg
                Level.Info -> AnsiColors.InfoFg
                Level.Warn -> AnsiColors.WarnFg
                Level.Error -> AnsiColors.ErrorFg
                Level.Off -> ""
            }
        }

        return "$bg$fg$text${AnsiColors.Reset}"
    }
}

/**
 * Format a throwable into a string with limits of [maxFrames] and [maxCauses].
 */
fun Throwable?.toLimitedString(
    maxFrames: Int = 8,
    maxCauses: Int = 3
): String {
    if (this == null) return "  (!) Exception not passed"

    fun Throwable.format(depth: Int): String {
        if (depth >= maxCauses) {
            return "        ... (cause chain truncated)\n"
        }

        val header = buildString {
            if (depth == 0) append("  (!) ")
            append("${this@format::class.simpleName}: $message")
        }

        val frames = stackTrace
            .take(maxFrames)
            .joinToString("\n") { "        at $it" }

        val more = stackTrace.size - maxFrames
        val moreLine = when {
            more <= 0 -> ""
            cause == null -> "        ... $more more"
            else -> "        ... $more more\n"
        }

        val causePart = cause?.let {
            "    Caused by: ${it.format(depth + 1)}"
        } ?: ""

        return buildString {
            appendLine(header)
            appendLine(frames)
            append(moreLine)
            append(causePart)
        }
    }

    return this.format(0)
}
