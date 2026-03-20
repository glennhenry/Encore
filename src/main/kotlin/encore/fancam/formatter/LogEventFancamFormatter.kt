package encore.fancam.formatter

import encore.startup.venue.EncoreFancamConfig
import encore.fancam.formatter.AnsiColors
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
            // [14:21:54.221](Application.kt:22)[D] <GameService> debug message
            return "[$timestamp]$source$level <${event.tag}> ${event.message()}"
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

        // [14:21:54.221](      Application.kt:22)[D] debug message
        return "[$timestamp]$source$level $message"
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
