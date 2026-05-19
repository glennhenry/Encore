package encore.utils.support

/**
 * Get the name of this class used for debugging or logging purposes.
 *
 * Anonymous object without name will returns "noname".
 */
fun Any?.debugName(): String {
    if (this == null) return "null<noname>"
    return this::class.simpleName ?: "noname"
}
