package encore.fancam.formatter

/**
 * Ansi colors (256) constants to style console.
 *
 * Must use "--console=plain" for color to work consistently.
 */
@Suppress("unused", "ConstPropertyName")
object AnsiColors {
    const val Esc = "\u001B"
    const val Reset = "$Esc[0m"

    const val BlackText = "$Esc[38;5;16m"

    const val TraceFg = "$Esc[38;5;66m"  // #5F8787 muted teal
    const val DebugFg = "$Esc[38;5;219m" // #FFAFFF bubble gum pink
    const val InfoFg = "$Esc[38;5;153m"  // #AFD7FF sky blue
    const val WarnFg = "$Esc[38;5;221m"  // #FFD75F dandelion
    const val ErrorFg = "$Esc[38;5;203m" // #FF5F5F pastel red

    const val TraceBg = "$Esc[48;5;66m"  // #5F8787 muted teal
    const val DebugBg = "$Esc[48;5;219m" // #FFAFFF bubble gum pink
    const val InfoBg = "$Esc[48;5;153m"  // #AFD7FF sky blue
    const val WarnBg = "$Esc[48;5;221m"  // #FFD75F dandelion
    const val ErrorBg = "$Esc[48;5;203m" // #FF5F5F pastel red

    fun fg(n: Int) = "$Esc[38;5;${n}m"
    fun bg(n: Int) = "$Esc[48;5;${n}m"
    fun bold(text: String) = "$Esc[1m$text$Esc[22m"
}