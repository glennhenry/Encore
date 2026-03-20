package encore.fancam.impl

import encore.fancam.Fancam
import encore.fancam.events.*
import io.ktor.util.date.*
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat

/**
 * A minimal fancam implementation intended for bootstrap or testing.
 *
 * This implementation is simple, lightweight, and dependency-less.
 *
 * - On each call to [trace], [debug], [info], [warn], [error], the log events
 *   are recorded. Likewise for [event] (whenever `log` is called) and
 *   [track] (whenever `log` or `record` is called).
 * - A `log()` call to track event does not get recorded to log event,
 *   but to track event instead.
 * - Provides access to them for assertion such as [takeLastLogTrace],
 *   [takeLastTrackTrace], [assertLogHas], [assertTrackHas].
 * - Minimal formatting: no color, never truncate.
 * - Does not implement file and client target.
 *
 * **Important**: if you are using `RehearsalFancam` to assert log calls,
 * you must still call the fancam initialization with the same object of `RehearsalFancam`.
 * This ensures the default `RehearsalFancam` on [Fancam], uses the same object.
 * Helper for this is available in `encoreTest.utils.TestFancam`.
 *
 * Example:
 * ```
 * // logevent
 * [12:32:11.444][D] message
 *
 * // trackevent
 * [12:32:11.444][D] { playerId: "abc", username: "def" }
 * ```
 */
class RehearsalFancam : FancamTemplate {
    private val tracelog = mutableListOf<LogEvent>()
    private val debuglog = mutableListOf<LogEvent>()
    private val infolog = mutableListOf<LogEvent>()
    private val warnlog = mutableListOf<LogEvent>()
    private val errorlog = mutableListOf<LogEvent>()

    private val tracetrack = mutableListOf<TrackEvent>()
    private val debugtrack = mutableListOf<TrackEvent>()
    private val infotrack = mutableListOf<TrackEvent>()
    private val warntrack = mutableListOf<TrackEvent>()
    private val errortrack = mutableListOf<TrackEvent>()

    private val date = SimpleDateFormat("HH:mm:ss.SSS")
    private val json = Json { prettyPrint = false }

    private fun formatLog(event: LogEvent): String {
        return "[${date.format(event.timestamp)}][${event.level.label()}] ${event.message()}"
    }

    private fun formatTrack(event: TrackEvent, level: Level): String {
        return "[${date.format(event.timestamp)}][${level.label()}] ${json.encodeToString(event.data)}"
    }

    override fun trace(tag: String, msg: () -> String) {
        create(msg, tag, Level.Trace).also {
            log(it)
        }
    }

    override fun debug(tag: String, msg: () -> String) {
        create(msg, tag, Level.Debug).also {
            log(it)
        }
    }

    override fun info(tag: String, msg: () -> String) {
        create(msg, tag, Level.Info).also {
            log(it)
        }
    }

    override fun warn(tag: String, msg: () -> String) {
        create(msg, tag, Level.Warn).also {
            log(it)
        }
    }

    override fun error(tag: String, msg: () -> String) {
        create(msg, tag, Level.Error).also {
            log(it)
        }
    }

    private fun create(msg: () -> String, tag: String, level: Level): LogEvent = LogEvent(
        message = msg,
        timestamp = getTimeMillis(),
        level = level,
        tag = tag,
        logFull = true,
        source = null,
        targetFile = null
    )

    private fun log(event: LogEvent, add: Boolean = true) {
        if (add) {
            addToLogEvent(event.level, event)
        }
        println(formatLog(event))
    }

    override fun event(level: Level, tag: String): LogEventBuilder {
        return LogEventBuilder(level, tag, null) {
            if (level == Level.Off) return@LogEventBuilder
            log(it)
        }
    }

    override fun track(name: String): TrackEventBuilder {
        return TrackEventBuilder(
            name = name,
            onRecordCalled = { println("TrackEvent.onRecordCalled: NOT IMPLEMENTED") },
            onLogCalled = { trackEvent, level, logFull ->
                addToTrackEvent(level, trackEvent)
                log(LogEvent(
                    message = { formatTrack(trackEvent, level) },
                    timestamp = trackEvent.timestamp,
                    level = level,
                    tag = trackEvent.tags.tagsToCommaSeparated(),
                    logFull = logFull,
                    source = trackEvent.source,
                    targetFile = trackEvent.route
                ), add = false)
            }
        )
    }

    private fun addToLogEvent(level: Level, event: LogEvent) {
        when (level) {
            Level.Trace -> tracelog.add(event)
            Level.Debug -> debuglog.add(event)
            Level.Info -> infolog.add(event)
            Level.Warn -> warnlog.add(event)
            Level.Error -> errorlog.add(event)
            Level.Off -> {}
        }
    }

    private fun addToTrackEvent(level: Level, event: TrackEvent) {
        when (level) {
            Level.Trace -> tracetrack.add(event)
            Level.Debug -> debugtrack.add(event)
            Level.Info -> infotrack.add(event)
            Level.Warn -> warntrack.add(event)
            Level.Error -> errortrack.add(event)
            Level.Off -> {}
        }
    }

    fun takeLastLogTrace(n: Int): List<LogEvent> = tracelog.takeLast(n)
    fun takeLastLogDebug(n: Int): List<LogEvent> = debuglog.takeLast(n)
    fun takeLastLogInfo(n: Int): List<LogEvent> = infolog.takeLast(n)
    fun takeLastLogWarn(n: Int): List<LogEvent> = warnlog.takeLast(n)
    fun takeLastLogError(n: Int): List<LogEvent> = errorlog.takeLast(n)

    fun takeLastTrackTrace(n: Int): List<TrackEvent> = tracetrack.takeLast(n)
    fun takeLastTrackDebug(n: Int): List<TrackEvent> = debugtrack.takeLast(n)
    fun takeLastTrackInfo(n: Int): List<TrackEvent> = infotrack.takeLast(n)
    fun takeLastTrackWarn(n: Int): List<TrackEvent> = warntrack.takeLast(n)
    fun takeLastTrackError(n: Int): List<TrackEvent> = errortrack.takeLast(n)

    /**
     * To assert whether any of the [lastN] message has log event of [level]
     * and match some predicate (e.g., string contains something).
     */
    fun assertLogHas(level: Level, lastN: Int, predicate: (String) -> Boolean): Boolean {
        val assert = when (level) {
            Level.Trace -> takeLastLogTrace(lastN).any { predicate(it.message()) }
            Level.Debug -> takeLastLogDebug(lastN).any { predicate(it.message()) }
            Level.Info -> takeLastLogInfo(lastN).any { predicate(it.message()) }
            Level.Warn -> takeLastLogWarn(lastN).any { predicate(it.message()) }
            Level.Error -> takeLastLogError(lastN).any { predicate(it.message()) }
            Level.Off -> { true }
        }

        if (!assert) {
            throw AssertionError("Failed to match predicate of $level in the last $lastN calls")
        } else {
            return true
        }
    }

    /**
     * To assert whether the [lastN] message has track event of [level]
     * and match some predicate (e.g., data map contains something).
     */
    fun assertTrackHas(level: Level, lastN: Int, predicate: (Map<String, Any>) -> Boolean): Boolean {
        val assert = when (level) {
            Level.Trace -> takeLastTrackTrace(lastN).any { predicate(it.data) }
            Level.Debug -> takeLastTrackDebug(lastN).any { predicate(it.data) }
            Level.Info -> takeLastTrackInfo(lastN).any { predicate(it.data) }
            Level.Warn -> takeLastTrackWarn(lastN).any { predicate(it.data) }
            Level.Error -> takeLastTrackError(lastN).any { predicate(it.data) }
            Level.Off -> { true }
        }

        if (!assert) {
            throw AssertionError("Failed to match predicate of $level in the last $lastN calls")
        } else {
            return true
        }
    }

    /**
     * Clear all saved log and track entries.
     */
    fun clearAll() {
        tracelog.clear()
        debuglog.clear()
        infolog.clear()
        warnlog.clear()
        errorlog.clear()
        tracetrack.clear()
        debugtrack.clear()
        infotrack.clear()
        warntrack.clear()
        errortrack.clear()
    }
}
