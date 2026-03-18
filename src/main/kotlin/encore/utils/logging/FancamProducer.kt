package encore.utils.logging

import encore.startup.venue.EncoreFancamConfig
import encore.utils.functions.toMB
import encore.utils.getRotatedFile
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream

interface FancamProducer<T> {
    val formatter: FancamFormatter<T>
    fun produce(event: T)
}

class ConsoleFancamProducer<T>(override val formatter: FancamFormatter<T>) : FancamProducer<T> {
    private val rawOut = PrintStream(FileOutputStream(FileDescriptor.out), true, Charsets.UTF_8)
    private fun println(msg: String) = rawOut.println(msg)

    override fun produce(event: T) {
        println(formatter.format(event))
    }
}

class FileFancamProducer<T>(
    private val config: EncoreFancamConfig,
    override val formatter: FancamFormatter<T>
) : FancamProducer<T> where T : FileRoutableEvent {
    override fun produce(event: T) {
        val filename = event.filename ?: return

        val file = getRotatedFile(
            directory = LOG_FILE_DIRECTORY,
            filename = filename,
            extension = LOG_FILE_EXTENSION,
            maxRotation = config.maxFileRotation
        ) { file ->
            file.length() > (config.maxFileSize.toMB())
        }

        val text = formatter.format(event)
        file.appendText("$text\n")
    }
}

class DisabledFancamProducer<T> : FancamProducer<T> {
    override val formatter: FancamFormatter<T>
        get() = error("This is not intended for usage.")

    override fun produce(event: T) = Unit
}
