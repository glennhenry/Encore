package encore.utils

/**
 * Get the name of this class.
 *
 * Anonymous object without name will returns "noname".
 */
fun Any?.name(): String {
    if (this == null) return "null<noname>"
    return this::class.simpleName ?: "noname"
}
