package encore.utils.constants

/**
 * Ansi colors (256) constants to style console.
 */
@Suppress("unused", "ConstPropertyName")
object AnsiColors {
    const val Esc = "\u001B"
    const val Reset = "$Esc[0m"

    const val BlackText = "$Esc[38;5;16m"
    const val BlackBg = "$Esc[48;5;16m"

    const val WhiteText = "$Esc[38;5;255m"
    const val WhiteBg = "$Esc[48;5;255m"

    const val VerboseFg = "$Esc[38;5;66m"
    const val DebugFg = "$Esc[38;5;219m"
    const val InfoFg = "$Esc[38;5;153m"
    const val WarnFg = "$Esc[38;5;221m"
    const val ErrorFg = "$Esc[38;5;203m"

    const val VerboseBg = "$Esc[48;5;66m"
    const val DebugBg = "$Esc[48;5;219m"
    const val InfoBg = "$Esc[48;5;153m"
    const val WarnBg = "$Esc[48;5;221m"
    const val ErrorBg = "$Esc[48;5;203m"

    fun fg(n: Int) = "$Esc[38;5;${n}m"
    fun bg(n: Int) = "$Esc[48;5;${n}m"
    fun bold(text: String) = "$Esc[1m$text$Esc[22m"
}