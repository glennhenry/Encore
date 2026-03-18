package encore.utils.logging

import encore.utils.AnyMapSerializerStrict
import kotlinx.serialization.Serializable

interface FileRoutableEvent {
    val filename: String?
}

@Serializable
data class TrackEvent(
    val name: String,
    val timestamp: Long,
    @Serializable(with = AnyMapSerializerStrict::class)
    val data: Map<String, Any>,
    val route: String,
    val tags: List<String>,
    val note: () -> String
) : FileRoutableEvent {
    override val filename: String
        get() = route
}

@Serializable
data class LogEvent(
    val message: () -> String,
    val timestamp: Long,
    val level: Level,
    val tag: String,
    val logFull: Boolean,
    val source: TraceElement?,
    val targetFile: String?
) : FileRoutableEvent {
    override val filename: String?
        get() = targetFile
}

@Serializable
data class TraceElement(
    val filename: String?,
    val lineNumber: Int
)

fun StackTraceElement?.toTraceElement(): TraceElement? {
    return this?.let {
        TraceElement(this.fileName, this.lineNumber)
    }
}
