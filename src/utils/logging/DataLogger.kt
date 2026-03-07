package utils.logging

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import utils.AnyMapSerializerReadable
import utils.AnyMapSerializerStrict
import java.io.File

const val TELEMETRY_DIRECTORY = ".telemetry"

/**
 * [Logger] abstraction for structured, data-oriented logging.
 *
 * `DataLogger` provides a simple DSL for logging structured key-value data
 * instead of raw text messages. It is built as an abstraction over the general [Logger]
 * to log data-heavy message in consistent format.
 *
 * Typical usage example:
 * ```kotlin
 * DataLogger.event("BuildingTask")
 *     .playerId("p1234")
 *     .username("PlayerABC")
 *     .data("buildingId", "bld_woodcutter_01")
 *     .data("durationSec", 120)
 *     .record()  // Writes to telemetry file: ".telemetry/BuildingTask.json"
 *     .log()     // Prints to console using the `Logger`
 * ```
 *
 * Each event is represented as a [DataLogBuilder], which can be:
 * - Logged directly to the console using [DataLogBuilder.log]
 * - Recorded persistently to telemetry files using [DataLogBuilder.record]
 */
object DataLogger {
    /**
     * Creates a new structured log event identified by [name],
     * returning a DSL [DataLogBuilder] for further configuration.
     *
     * The provided [name] will also be used as the filename if telemetry [DataLogBuilder.record] is invoked.
     *
     * Example:
     * ```kotlin
     * val log = DataLogger.event("PlayerLogin")
     *     .playerId("p1234")
     *     .data("ip", "127.0.0.1")
     *     .log()
     * ```
     */
    fun event(name: String) = DataLogBuilder(name)
}

/**
 * A DSL-style builder used to construct structured log events.
 *
 * Each builder corresponds to a single telemetry/log event
 * and supports both console logging and file-based recording.
 */
class DataLogBuilder(private val name: String) {
    private val data = mutableMapOf<String, Any>()
    private var playerId: String = "[Undefined]"
    private var username: String = "[Undefined]"
    private var text: String = ""
    private val logJsonBuilder = Json { prettyPrint = false }
    private val recordJsonBuilder = Json { prettyPrint = true }

    /**
     * Sets the `playerId` associated with this log entry.
     *
     * If not provided, defaults to `"[Undefined]"`.
     */
    fun playerId(id: String) = apply { playerId = id }

    /**
     * Sets the `username` associated with this log entry.
     *
     * If not provided, defaults to `"[Undefined]"`.
     */
    fun username(name: String) = apply { username = name }

    /**
     * Set prefix text before the data logs.
     */
    fun prefixText(text: String) = apply { this.text = text }

    /**
     * Adds or overwrites a key-value pair in this log entry.
     *
     * Inputting `playerId` as key will automatically overwrite the [playerId].
     */
    fun data(key: String, value: Any) = apply {
        if (key.equals("playerId", ignoreCase = true)) {
            playerId = value.toString()
        } else if (key.equals("username", ignoreCase = true)) {
            username = value.toString()
        } else {
            data[key] = value
        }
    }

    /**
     * Finalizes the current log data and outputs it using the general [Logger].
     *
     * Uses [LogLevel.Info] by default.
     * Output is formatted as a single structured string (non-JSON).
     */
    fun log(level: LogLevel = LogLevel.Info, textOnly: Boolean = false) {
        val msg = buildString(textOnly)
        when (level) {
            LogLevel.Verbose -> Logger.verbose(logFull = true) { msg }
            LogLevel.Debug -> Logger.debug(logFull = true) { msg }
            LogLevel.Info -> Logger.info(logFull = true) { msg }
            LogLevel.Warn -> Logger.warn(logFull = true) { msg }
            LogLevel.Error -> Logger.error(logFull = true) { msg }
            LogLevel.Nothing -> {}
        }
    }

    /**
     * Persists the structured log as a telemetry record.
     *
     * Each event is saved under the `.telemetry/` directory, using the event [name]
     * as the filename (e.g. `"BuildingTask.json"`).
     *
     * The output is stored in JSON format, one entry per line.
     *
     * This method does **not** finalize the DSL — you can still add more data or call [log] afterward.
     */
    fun record() = apply {
        val dir = File(TELEMETRY_DIRECTORY)
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "$name.json")
        val newJson = asJson()

        if (!file.exists() || file.readBytes().isEmpty()) {
            file.writeText("[\n$newJson\n]")
        } else {
            val content = file.readText().trim()
            if (content.endsWith("]")) {
                val updated = buildString {
                    append(content.dropLast(1).trimEnd())
                    if (content.length > 2) append(",\n")
                    append(newJson)
                    append("\n]")
                }
                file.writeText(updated)
            } else {
                Logger.warn { "JSON corruption detected on file .telemetry/${file.name}, new JSON data will be appended directly" }
                file.appendText("[\n$newJson\n]")
            }
        }
    }

    /**
     * Builds a formatted string version of the structured log data,
     * intended for human-readable console output.
     * @param textOnly Whether the message should including event name
     *                 like `[Event:ExampleDataLogging] prefix text goes here`.
     */
    fun buildString(textOnly: Boolean): String {
        val content = buildString {
            if (textOnly) {
                append("$text ")
            } else {
                append("[Event:$name] $text ")
            }
            append(logJsonBuilder.encodeToString(LogEvent(playerId, username, data)))
        }
        return content
    }

    /**
     * Converts the log event into a structured JSON object.
     *
     * This is the preferred representation for telemetry recording.
     */
    fun asJson(): String {
        return recordJsonBuilder.encodeToString(TelemetryEvent(name, playerId, username, data))
    }
}

@Serializable
data class TelemetryEvent(
    val event: String,
    val playerId: String,
    val username: String,
    @Serializable(with = AnyMapSerializerStrict::class)
    val data: Map<String, Any> = emptyMap()
)

@Serializable
data class LogEvent(
    val playerId: String,
    val username: String,
    @Serializable(with = AnyMapSerializerReadable::class)
    val data: Map<String, Any> = emptyMap()
)
