package encore.utils.logging

enum class Level {
    Trace, Debug, Info, Warn, Error, Off
}

fun String.toLogLevel(): Level {
    return when (this.lowercase()) {
        "trace" -> Level.Trace
        "debug" -> Level.Debug
        "info" -> Level.Info
        "warn" -> Level.Warn
        "error" -> Level.Error
        "off" -> Level.Off
        else -> Level.Trace
    }
}

fun Level.label(): String {
    return when (this) {
        Level.Trace -> "T"
        Level.Debug -> "D"
        Level.Info -> "I"
        Level.Warn -> "W"
        Level.Error -> "E"
        Level.Off -> "OFF"
    }
}
