package encore.utils.logging

import encore.startup.venue.EncoreFancamConfig
import io.ktor.util.date.getTimeMillis
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat

/**
 * A minimal fancam implementation intended for testing.
 *
 * - On each call to [trace], [debug], [info], [warn], [error], the log events
 *   are recorded. Likewise for [event] (whenever `log` is called) and
 *   [track] (whenever `log` or `record`is called).
 * - Provides access to them for assertion such as [getLastLogTrace],
 *   [getLastTrackTrace], [assertLogHas], [assertTrackHas]
 * - Minimal formatting: no color, never truncate.
 * - Does not take account [EncoreFancamConfig].
 * - Does not implement file and client target.
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
            tracelog.add(it)
            log(it)
        }
    }

    override fun debug(tag: String, msg: () -> String) {
        create(msg, tag, Level.Debug).also {
            debuglog.add(it)
            log(it)
        }
    }

    override fun info(tag: String, msg: () -> String) {
        create(msg, tag, Level.Info).also {
            infolog.add(it)
            log(it)
        }
    }

    override fun warn(tag: String, msg: () -> String) {
        create(msg, tag, Level.Warn).also {
            warnlog.add(it)
            log(it)
        }
    }

    override fun error(tag: String, msg: () -> String) {
        create(msg, tag, Level.Error).also {
            errorlog.add(it)
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

    private fun log(event: LogEvent) {
        println(formatLog(event))
    }

    override fun event(level: Level, tag: String): LogEventBuilder {
        return LogEventBuilder(level, tag, null) {
            if (level == Level.Off) return@LogEventBuilder
            when (level) {
                Level.Trace -> tracelog.add(it)
                Level.Debug -> debuglog.add(it)
                Level.Info -> infolog.add(it)
                Level.Warn -> warnlog.add(it)
                Level.Error -> errorlog.add(it)
            }
            log(it)
        }
    }

    override fun track(name: String): TrackEventBuilder {
        return TrackEventBuilder(
            name = name,
            onRecordCalled = { println("TrackEvent.onRecordCalled: NOT IMPLEMENTED") },
            onLogCalled = { trackEvent, level, logFull ->
                when (level) {
                    Level.Trace -> tracetrack.add(trackEvent)
                    Level.Debug -> debugtrack.add(trackEvent)
                    Level.Info -> infotrack.add(trackEvent)
                    Level.Warn -> warntrack.add(trackEvent)
                    Level.Error -> errortrack.add(trackEvent)
                    Level.Off -> {}
                }

                LogEvent(
                    message = { formatTrack(trackEvent, level) },
                    timestamp = trackEvent.timestamp,
                    level = level,
                    tag = trackEvent.tags.tagsToCommaSeparated(),
                    logFull = logFull,
                    source = trackEvent.source,
                    targetFile = trackEvent.route
                )
            }
        )
    }

    fun getLastLogTrace(n: Int): List<LogEvent> = tracelog.takeLast(n)
    fun getLastLogDebug(n: Int): List<LogEvent> = debuglog.takeLast(n)
    fun getLastLogInfo(n: Int): List<LogEvent> = infolog.takeLast(n)
    fun getLastLogWarn(n: Int): List<LogEvent> = warnlog.takeLast(n)
    fun getLastLogError(n: Int): List<LogEvent> = errorlog.takeLast(n)

    fun getLastTrackTrace(n: Int): List<TrackEvent> = tracetrack.takeLast(n)
    fun getLastTrackDebug(n: Int): List<TrackEvent> = debugtrack.takeLast(n)
    fun getLastTrackInfo(n: Int): List<TrackEvent> = infotrack.takeLast(n)
    fun getLastTrackWarn(n: Int): List<TrackEvent> = warntrack.takeLast(n)
    fun getLastTrackError(n: Int): List<TrackEvent> = errortrack.takeLast(n)

    /**
     * To assert whether any of the [lastN] message has log event of [level]
     * and match some predicate (e.g., string contains something).
     */
    fun assertLogHas(level: Level, lastN: Int, predicate: (String) -> Boolean) {
        when (level) {
            Level.Trace -> getLastLogTrace(lastN).any { predicate(it.message()) }
            Level.Debug -> getLastLogDebug(lastN).any { predicate(it.message()) }
            Level.Info -> getLastLogInfo(lastN).any { predicate(it.message()) }
            Level.Warn -> getLastLogWarn(lastN).any { predicate(it.message()) }
            Level.Error -> getLastLogError(lastN).any { predicate(it.message()) }
            Level.Off -> {}
        }
    }

    /**
     * To assert whether the [lastN] message has track event of [level]
     * and match some predicate (e.g., data map contains something).
     */
    fun assertTrackHas(level: Level, lastN: Int, predicate: (Map<String, Any>) -> Boolean) {
        when (level) {
            Level.Trace -> getLastTrackTrace(lastN).any { predicate(it.data) }
            Level.Debug -> getLastTrackDebug(lastN).any { predicate(it.data) }
            Level.Info -> getLastTrackInfo(lastN).any { predicate(it.data) }
            Level.Warn -> getLastTrackWarn(lastN).any { predicate(it.data) }
            Level.Error -> getLastTrackError(lastN).any { predicate(it.data) }
            Level.Off -> {}
        }
    }
}
