package encore.utils.logging

const val LOG_FILE_EXTENSION = "log"
const val TRACK_FILE_EXTENSION = "jsonl"
const val LOG_FILE_DIRECTORY = ".logs"

object Fancam {
    private var initialized = false
    private lateinit var fancam: FancamTemplate

    fun initialize(fancam: FancamTemplate) {
        if (initialized) {
            warn { "Fancam is already initialized." }
            return
        }
        this.fancam = fancam
    }

    fun trace(tag: String = "", msg: () -> String) = fancam.trace(tag, msg)
    fun debug(tag: String = "", msg: () -> String) = fancam.debug(tag, msg)
    fun info(tag: String = "", msg: () -> String) = fancam.info(tag, msg)
    fun warn(tag: String = "", msg: () -> String) = fancam.warn(tag, msg)
    fun error(tag: String = "", msg: () -> String) = fancam.error(tag, msg)

    fun event(level: Level, tag: String = ""): LogEventBuilder = fancam.event(level, tag)
    fun track(name: String): TrackEventBuilder = fancam.track(name)
}
