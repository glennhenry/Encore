package encore.utils.logging

import encore.startup.venue.EncoreFancamConfig
import encore.utils.functions.toMB
import encore.utils.getRotatedFile
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream

/**
 * Represent a producer for log or track event.
 *
 * Implementation defines how a log or track event should be outputted
 * into a particular target.
 *
 * Examples:
 * - [ConsoleFancamProducer]
 * - [FileFancamProducer]
 *
 * @param T Generic type which should be limited to [LogEvent] or [TrackEvent].
 */
interface FancamProducer<T> {
    fun produce(event: T)
}

/**
 * Producer implementation tailored for console output.
 *
 * A formatter is expected for this implementation, which may be
 * - [LogEventFancamFormatter]
 *
 * Produce is implemented with `println` call through [PrintStream].
 */
class ConsoleFancamProducer<T>(private val formatter: FancamFormatter<T>) : FancamProducer<T> {
    private val rawOut = PrintStream(FileOutputStream(FileDescriptor.out), true, Charsets.UTF_8)
    private fun println(msg: String) = rawOut.println(msg)

    override fun produce(event: T) {
        println(formatter.format(event))
    }
}

/**
 * Producer implementation tailored for file output.
 *
 * A formatter is expected for this implementation, which may be
 * - [LogEventFancamFormatter]
 * - [TrackEventFancamFormatter]
 *
 * Produce is implemented by appending the result string of the formatter
 * into the event's target file with rotation enabled.
 */
class FileFancamProducer<T>(
    private val config: EncoreFancamConfig,
    private val formatter: FancamFormatter<T>
) : FancamProducer<T> where T : FileRoutableEvent {
    override fun produce(event: T) {
        val filename = event.filename ?: return

        val file = getRotatedFile(
            directory = LOG_FILE_DIRECTORY,
            filename = filename,
            extension = event.extension,
            maxRotation = config.maxFileRotation
        ) { file ->
            file.length() > (config.maxFileSize.toMB())
        }

        val text = formatter.format(event)
        file.appendText("$text\n")
    }
}
