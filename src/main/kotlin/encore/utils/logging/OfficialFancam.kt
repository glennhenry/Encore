package encore.utils.logging

import encore.startup.venue.EncoreFancamConfig
import encore.utils.StackTraceResolver
import io.ktor.util.date.getTimeMillis
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

/**
 * Default implementation of fancam template.
 *
 * ## Overview
 *
 * 1. Supports basic logging through [trace], [debug], [info], [warn], and [error].
 * 2. Logging call can be done through builder DSL through [event].
 * 3. Structured logging uses the [track] method.
 *
 * Require [EncoreFancamConfig] to configure the logger behavior.
 *
 * ## Usage
 *
 * ### 1. Basic logging
 *
 * Any simple print at various level with categorical info tag:
 * ```kotlin
 * const val ServiceTag = "Service"
 * const val MultipleTag = "Game,Player,Inventory"
 *
 * Fancam.warn(ServiceTag) { "Service x under problem." }
 * Fancam.warn(MultipleTag) { "Service x under problem." }
 * ```
 *
 * ### 2. Advanced logging with builder DSL
 *
 * For more advanced config such as file target and to toggle [LogEvent.logFull].
 * ```kotlin
 * fancam.event(Level.Info, "InventoryService")
 *       .message { "service working..." }
 *       .toFile("InventoryService")
 *       .log(full = true)
 * ```
 *
 * ### 3. Structured logging with builder DSL
 *
 * To write a structured log of [TrackEvent].
 * ```kotlin
 * // recorded to default file `events.jsonl`, not logged
 * Fancam.track("SystemHealth")
 *       .data("heat", 12)
 *       .data("playerOnline", 100)
 *       .tags("system", "metric")
 *       .note { if (heat > 10) { "WARNING OVERHEAT" } else "" }
 *       .record()
 *
 * // recorded to `loots.jsonl`, logged with info level
 * Fancam.track("PlayerLoot")
 *       .playerId("pid123")
 *       .username("playerABC")
 *       .data("loot", "bread")
 *       .tags("player", "rng")
 *       .route("loots")
 *       .record()
 *       .log(Level.Info, full = false)
 * ```
 *
 * ## Features
 *
 * ### Beautiful log
 *
 * - Display a formatted timestamp of when the log call was invoked.
 * - Show a hyperlink to the source file and line number of the log caller.
 * - Allow ANSI coloring for the level label with [EncoreFancamConfig.colorEnabled].
 * - Truncation of log message (per-newline) depending on [LogEvent.logFull]
 *   and [EncoreFancamConfig.maxLineLength].
 *
 * ### Three output targets
 *
 * Any log call is printed to the console, optionally to the file via [LogEvent.targetFile]
 * or for structured logging via [TrackEvent.route]. Log events are also sent to client
 * via ... (todo).
 *
 * #### Client output
 *
 * An external web client to see advanced version of the logger is available at ...
 * Allows for filtration, search, and etc.
 *
 * ## Implementation Details
 *
 * Uses a separate thread to manage the order and execution of logging calls.
 *
 * Below is the spec of [FancamFormatter] and [FancamProducer] used for
 * log or track event and console or file output.
 *
 * 1. `LogEvent to Console`   : [LogEventFancamFormatter] + [ConsoleFancamProducer]
 * 2. `LogEvent to File`      : [LogEventFancamFormatter] (with `isFileTarget` flag) + [FileFancamProducer]
 * 3. `TrackEvent to Console` : [ConsoleTrackEventFancamFormatter] + [ConsoleFancamProducer] (reuse)
 * 4. `TrackEvent to File`    : [TrackEventFancamFormatter] + [FileFancamProducer]
 *
 */
class OfficialFancam(private val config: EncoreFancamConfig) : FancamTemplate {
    private val sourceResolver: StackTraceResolver = StackTraceResolver()

    // LogEvent formatters
    private val consoleLogFormatter = LogEventFancamFormatter(config, isFileTarget = false)
    private val fileLogFormatter = LogEventFancamFormatter(config, isFileTarget = true)

    // TrackEvent formatters
    private val consoleTrackFormatter = ConsoleTrackEventFancamFormatter()
    private val fileTrackFormatter = TrackEventFancamFormatter()

    // LogEvent producers
    private val consoleLogProducer = ConsoleFancamProducer(consoleLogFormatter)
    private val fileLogProducer = FileFancamProducer(config, fileLogFormatter)

    // TrackEvent producers
    // consoleTrackProducer reuses consoleLogProducer
    private val fileTrackProducer = FileFancamProducer(config, fileTrackFormatter)

    override fun trace(tag: String, msg: () -> String) {
        if (Level.Trace < config.level.toLogLevel()) return
        log(create(msg, tag, Level.Trace))
    }

    override fun debug(tag: String, msg: () -> String) {
        if (Level.Debug < config.level.toLogLevel()) return
        log(create(msg, tag, Level.Debug))
    }

    override fun info(tag: String, msg: () -> String) {
        if (Level.Info < config.level.toLogLevel()) return
        log(create(msg, tag, Level.Info))
    }

    override fun warn(tag: String, msg: () -> String) {
        if (Level.Warn < config.level.toLogLevel()) return
        log(create(msg, tag, Level.Warn))
    }

    override fun error(tag: String, msg: () -> String) {
        if (Level.Error < config.level.toLogLevel()) return
        log(create(msg, tag, Level.Error))
    }

    private fun create(msg: () -> String, tag: String, level: Level): LogEvent = LogEvent(
        message = msg,
        timestamp = getTimeMillis(),
        level = level,
        tag = tag,
        logFull = false,
        source = sourceResolver.resolve(),
        targetFile = null
    )

    private fun log(event: LogEvent) {
        eventQueue.offer(LogQueueEvent(event, fromTrackEvent = false))
    }

    override fun event(level: Level, tag: String): LogEventBuilder {
        return LogEventBuilder(level, tag, sourceResolver.resolve()) {
            if (it.level == Level.Off) {
                warn { "Log with Level.Off is not intended to be used." }
            } else {
                log(it)
            }
        }
    }

    override fun track(name: String): TrackEventBuilder {
        return TrackEventBuilder(
            name = name,
            onRecordCalled = { eventQueue.offer(TrackQueueEvent(it)) },
            onLogCalled = { trackEvent, level, logFull ->
                eventQueue.offer(
                    LogQueueEvent(
                        LogEvent(
                            message = { consoleTrackFormatter.format(trackEvent) },
                            timestamp = trackEvent.timestamp,
                            level = level,
                            tag = trackEvent.tags.tagsToCommaSeparated(),
                            logFull = logFull,
                            source = trackEvent.source,
                            targetFile = trackEvent.route
                        ), fromTrackEvent = true
                        // track event is only outputted to file via onRecordCalled
                        // fromTrackEvent flag prevent the console log event
                        // from producing file output
                    )
                )
            }
        )
    }

    // blocking queue of log calls processed by a separate thread
    // ensuring proper log call ordering while not blocking main thread
    private val eventQueue = LinkedBlockingQueue<QueueEvent>()
    private val executor = Executors.newSingleThreadExecutor()

    init {
        startConsumer()
    }

    private fun startConsumer() {
        executor.execute {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val event = eventQueue.take()
                    process(event)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    Thread.currentThread().interrupt()
                }
            }
        }
    }

    private fun process(event: QueueEvent) {
        try {
            when (event) {
                is LogQueueEvent -> {
                    val log = event.event
                    consoleLogProducer.produce(log)
                    if (log.filename != null && !event.fromTrackEvent) {
                        fileLogProducer.produce(log)
                    }
                }

                is TrackQueueEvent -> {
                    fileTrackProducer.produce(event.event)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

sealed interface QueueEvent
data class LogQueueEvent(val event: LogEvent, val fromTrackEvent: Boolean) : QueueEvent
data class TrackQueueEvent(val event: TrackEvent) : QueueEvent
