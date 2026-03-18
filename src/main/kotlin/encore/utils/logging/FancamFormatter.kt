package encore.utils.logging

import encore.startup.venue.EncoreFancamConfig
import encore.utils.constants.AnsiColors
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat

interface FancamFormatter<T> {
    fun format(event: T): String
}

class LogEventFancamFormatter(
    private val config: EncoreFancamConfig,
    private val colorize: Boolean
) : FancamFormatter<LogEvent> {
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS")

    override fun format(event: LogEvent): String {
        val timestamp = formatTimestamp(event.timestamp, timeFormat)
        val source = formatSourceHint(event.source, config.fileNamePadding)
        val level = if (colorize) {
            colorizeText(event.level, event.level.label())
        } else {
            event.level.label()
        }

        return "[$timestamp]($source)[$level] ${event.message()}"
    }

    private fun formatTimestamp(timestamp: Long, format: SimpleDateFormat): String {
        return format.format(timestamp)
    }

    private fun formatSourceHint(source: TraceElement?, fileNamePadding: Int): String {
        if (source == null) return "(UnknownSource)"
        val file = source.filename
        val line = source.lineNumber

        if (file == null) return "(UnknownFilename)"

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

class TrackEventFancamFormatter(private val usePrettyPrint: Boolean) : FancamFormatter<TrackEvent> {
    private val jsonSerializer = Json { prettyPrint = usePrettyPrint }

    override fun format(event: TrackEvent): String {
        return jsonSerializer.encodeToString(event)
    }
}
