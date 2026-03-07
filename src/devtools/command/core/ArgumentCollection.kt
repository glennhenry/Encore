package devtools.command.core

/**
 * A wrapper around the list of raw argument strings provided to a command.
 *
 * Arguments are consumed in order. Convenience methods are provided to
 * retrieve and convert values:
 * - [next] for raw strings
 * - [nextInt], [nextBoolean], [nextDouble] for typed values
 * - each type also offers a `default` variant for optional retrieval
 *
 * Arguments that cannot be converted to the requested type will return null.
 */
class ArgumentCollection(private val arguments: List<String>) {
    private var index = 0

    /**
     * Get the next argument as String.
     */
    fun next(): String? = arguments.getOrNull(index).also { index++ }
    fun nextInt(): Int? = next()?.toIntOrNull()
    fun nextIntOr(default: Int): Int = nextInt() ?: default
    fun nextBoolean(): Boolean? = next()?.toBooleanStrictOrNull()
    fun nextBooleanOr(default: Boolean): Boolean = nextBoolean() ?: default
    fun nextDouble(): Double? = next()?.toDoubleOrNull()
    fun nextDoubleOr(default: Double): Double = nextDouble() ?: default

    override fun equals(other: Any?): Boolean {
        return other is ArgumentCollection && this.arguments.zip(other.arguments).all { (a, b) -> a == b }
    }

    override fun hashCode(): Int {
        return 31 * index + arguments.hashCode()
    }

    override fun toString(): String {
        return arguments.joinToString(" ")
    }
}
