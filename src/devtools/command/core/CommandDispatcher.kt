package devtools.command.core

import annotation.RevisitLater
import context.ServerContext
import utils.logging.ILogger
import utils.logging.Logger

/**
 * Dispatch and execute server registered commands.
 *
 * Server commands offers the ability to control the server from command implementation.
 * It gives the possibility to modify server's behavior, such as modifying player's data.
 *
 * The server accepts command from simple CLI via the external web devtools.
 *
 * How to use:
 * - Implement [Command].
 * - Register the command with [register].
 * - Via the devtools, type the command with input arguments.
 * - Syntax typically looks like 'give playerABC water 100'.
 *
 * See example in `test.devtools.CommandDispatcherTest`.
 */
class CommandDispatcher(private val logger: ILogger) {
    private lateinit var serverContext: ServerContext
    private val commands = mutableMapOf<String, Command>()
    private val parser = CommandParser()

    /**
     * Require at least one character that is not a whitespace.
     * Allows only: [[a-z]] [[A-Z]] [[0-9]] [[-]] [[_]] and whitespace
     */
    private val allowedPattern = Regex("^(?=.*\\S)[a-zA-Z0-9_\\-\\s]+$")

    /**
     * Register a server command.
     *
     * @param command The command to be registered.
     *
     * @throws IllegalArgumentException throws when:
     * - `commandId` is blank or contains invalid character (see [allowedPattern]).
     * - Command has duplicate [CommandVariant].
     */
    @RevisitLater(
        "It is not possible to enforce unique variants on command, " +
                "since variants are implemented by the command itself. " +
                "As a result, we chose to throw exception on registration. " +
                "We may not want random error for something as trivial as a command registration."
    )
    fun register(command: Command) {
        val cleanId = sanitizeCommandId(command.commandId)

        if (cleanId in commands) {
            logger.warn { "The commandId '${cleanId}' has been registered before, the old one will be overwritten." }
        }

        val seenVariant = mutableMapOf<Int, CommandVariant>()
        for (variant in command.variants) {
            if (variant.argCount in seenVariant) {
                throw IllegalArgumentException(
                    "\n\tFound duplicate command variant for command '${cleanId}' " +
                            "with argument length of ${variant.argCount}: $variant\n" +
                            "\tVariants must have unique argument counts.\n" +
                            "\tThe first-come variant: (${seenVariant[variant.argCount]?.detailedString()}) will be used"
                )
            } else {
                seenVariant[variant.argCount] = variant
            }
        }

        commands[cleanId] = command
    }

    private fun sanitizeCommandId(id: String): String {
        // 1. Empty commandId
        if (id.isBlank()) {
            throw IllegalArgumentException("commandId is blank.")
        }

        // 2. Command does not match regex
        if (!id.matches(allowedPattern)) {
            val badChars = id.filter { !it.isLetterOrDigit() && it !in "_- " }
            throw IllegalArgumentException(
                "commandId contains invalid characters: '$badChars'. Allowed: letters, digits, '-', '_', and spaces."
            )
        }

        return id.trim()
    }

    /**
     * Handle raw command request of plain string.
     *
     * Internally parse the raw command and use the [handleCommand].
     *
     * @return [CommandResult] that represents the outcome.
     */
    fun handleRawCommand(raw: String): CommandResult {
        val request = try {
            parser.parse(raw)
        } catch (e: IllegalArgumentException) {
            // just parse error which is not severe
            return CommandResult.Error("Parsing error: ${e.message}")
        } catch (e: Exception) {
            Logger.error { "Unexpected parsing error on handleRawCommand: ${e.message}" }
            return CommandResult.Error("Parsing error: ${e.message}")
        }

        return handleCommand(request)
    }

    /**
     * Handle command request.
     *
     * It involves doing a lookup to the registered commands, then
     * calling `execute` method in the command implementation.
     *
     * @return [CommandResult] that represents the outcome.
     */
    fun handleCommand(request: CommandRequest): CommandResult {
        val command = commands[request.commandId] ?: return CommandResult.CommandNotFound(request.commandId)

        logger.info { "Received command '${request.commandId} ${request.arguments}'" }

        try {
            val result = command.execute(serverContext, request.arguments)
            Logger.info { "Done executing command '${request.commandId}' with result=$result" }
            return result
        } catch (e: Exception) {
            val msg = "Unexpected error while executing the command '${request.commandId}'; error: ${e.message ?: e}"
            Logger.error { msg }
            return CommandResult.Error(msg)
        }
    }

    /**
     * @return [Set] of registered `commandId`(s).
     */
    fun getAllRegisteredCommandsId(): Set<String> {
        return commands.keys
    }

    /**
     * @return [List] of variants of `commandId`. Returns empty list if the command is not registered.
     */
    fun getAllVariantsOf(commandId: String): List<CommandVariant> {
        return commands[commandId]?.variants ?: emptyList()
    }

    /**
     * @return [Set] of registered commands.
     */
    fun getAllRegisteredCommands(): Set<Command> {
        return commands.values.toSet()
    }

    fun init(context: ServerContext) {
        this.serverContext = context
    }
}
