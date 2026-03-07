package devtools.command.core

/**
 * Represents a specific variant of a command.
 *
 * The command system ignore types and rely on variant's arguments length to
 * distinguish between different command variant. As a result, each variant
 * must have unique argument counts.
 *
 * Allowed:
 * - (playerId, itemId)
 * - (playerId, itemId, amount)
 *
 * Not allowed:
 * - (playerId, itemId)
 * - (playerName, itemName)  // same length although different type → ambiguous
 *
 * Variant with same argument length can fallback by making a new command (e.g., give and give-ext)
 *
 * @property signature The list of argument descriptors, in the exact
 *                     order they are expected to appear when the command is executed.
 */
data class CommandVariant(
    val signature: List<ArgumentDescriptor>
) {
    val argCount: Int
        get() = signature.size

    override fun toString(): String {
        return "(" + signature.joinToString(", ") { it.id } + ")"
    }

    fun detailedString(): String {
        return signature.joinToString(", ") { it.toString() }
    }
}

fun List<CommandVariant>.variantsAsString(): String {
    return this.joinToString("|") { it.toString() }
}
