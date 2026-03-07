package utils.constants

/**
 * Ansi colors (256) constants to style console.
 */
@Suppress("unused", "ConstPropertyName")
object AnsiColors {
    const val Reset = "\u001B[0m"

    const val BlackText = "\u001B[38;5;16m"
    const val BlackBg = "\u001B[48;5;16m"

    const val WhiteText = "\u001B[38;5;255m"
    const val WhiteBg = "\u001B[48;5;255m"

    const val VerboseFg = "\u001B[38;5;66m"
    const val DebugFg = "\u001B[38;5;219m"
    const val InfoFg = "\u001B[38;5;153m"
    const val WarnFg = "\u001B[38;5;221m"
    const val ErrorFg = "\u001B[38;5;203m"

    const val VerboseBg = "\u001B[48;5;66m"
    const val DebugBg = "\u001B[48;5;219m"
    const val InfoBg = "\u001B[48;5;153m"
    const val WarnBg = "\u001B[48;5;221m"
    const val ErrorBg = "\u001B[48;5;203m"

    fun fg(n: Int) = "\u001B[38;5;${n}m"
    fun bg(n: Int) = "\u001B[48;5;${n}m"
    fun bold(text: String) = "\u001B[1m$text\u001B[22m"
}