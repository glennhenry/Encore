package encore.backstage.command

import encore.backstage.command.types.ArgumentCollection
import encore.backstage.command.types.CommandResult
import encore.backstage.command.types.CommandVariant
import encore.context.ServerContext

/**
 * Represents a server command that can be invoked to perform a specific action in the server.
 *
 * See [ExampleCommand] or `encoreTest.backstage.CommandDispatcherTest` for example.
 */
interface Command {
    /**
     * A human-readable name for the command which is also used to call the command.
     * Must be unique to other commands. Case-sensitive.
     */
    val commandId: String

    /**
     * An explanation of what the command does.
     */
    val description: String

    /**
     * Defines the possible variants for this command.
     */
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
