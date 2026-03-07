package devtools.command.core

import context.ServerContext

/**
 * Represents a server command that can be invoked to perform a specific action in server.
 *
 * See `test.devtools.CommandDispatcherTest` for example.
 *
 * @property commandId A human-readable name for the command which is also used to call the command.
 *                     Must be unique to other commands. Case-sensitive.
 * @property description An explanation of what the command does.
 */
interface Command {
    val commandId: String
    val description: String
    val variants: List<CommandVariant>

    /**
     * Execution logic of the command.
     *
     * @param serverContext The server's state to be used during execution.
     * @param args Input arguments object.
     *
     * @return Result of command execution, which should be any of the three:
     * - [CommandResult.Executed]
     * - [CommandResult.ExecutionFailure]
     * - [CommandResult.Error]
     */
    fun execute(serverContext: ServerContext, args: ArgumentCollection): CommandResult
}
