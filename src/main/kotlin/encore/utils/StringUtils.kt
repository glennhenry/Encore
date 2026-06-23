package encore.utils

/**
 * Break the string into multiple lines.
 *
 * For example, `string.breakIntoLines(maxCharsEachLine=5, lineIndent=2)`
 * transforms into:
 * ```
 * abcdefghijklmnopqrstuvwxyz (len=26)
 *
 * abcde
 *   fghij
 *   klmno
 *   pqrst
 *   uvwxy
 *   z
 * ```
 *
 * @param maxCharsEachLine the max chars for each line.
 * @param lineIndent optional amount of spaces to put on each newline.
 */
fun String.breakIntoLines(maxCharsEachLine: Int, lineIndent: Int = 0): String {
    require(maxCharsEachLine > 0)
    val result = StringBuilder(length + length / maxCharsEachLine)

    var start = 0
    while (start < length) {
        val end = minOf(start + maxCharsEachLine, length)
        if (start != 0 && lineIndent != 0) {
            result.append(" ".repeat(lineIndent))
        }
        result.append(this, start, end)
        if (end < length) {
            result.append('\n')
        }
        start = end
    }

    return result.toString()
}
