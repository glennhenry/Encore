package devtools.command.core

/**
 * Represent a single command request of [commandId] with the input of [arguments].
 */
data class CommandRequest(
    val commandId: String,
    val arguments: ArgumentCollection
)
