package encore.utils.logging

import encore.startup.venue.EncoreFancamConfig
import io.ktor.util.date.getTimeMillis

class OfficialFancam(override val config: EncoreFancamConfig) : FancamTemplate {
    override val sourceResolver: FancamSourceResolver = OfficialFancamSourceResolver()

    // LogEvent formatters
    private val consoleLogFormatter = LogEventFancamFormatter(config, colorize = config.colorEnabled)
    private val fileLogFormatter = LogEventFancamFormatter(config, colorize = false)

    // TrackEvent formatters
    private val consoleTrackFormatter = TrackEventFancamFormatter(usePrettyPrint = false)
    private val fileTrackFormatter = TrackEventFancamFormatter(usePrettyPrint = true)

    // LogEvent producers
    private val consoleLogProducer = ConsoleFancamProducer(consoleLogFormatter)
    private val fileLogProducer = FileFancamProducer(config, fileLogFormatter)

    // TrackEvent producers
    private val consoleTrackProducer = ConsoleFancamProducer(consoleTrackFormatter)
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
        consoleLogProducer.produce(event)
        if (event.filename != null) {
            fileLogProducer.produce(event)
        }
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
            onRecordCalled = { fileTrackProducer.produce(it) },
            onLogCalled = { consoleTrackProducer.produce(it) }
        )
    }
}
