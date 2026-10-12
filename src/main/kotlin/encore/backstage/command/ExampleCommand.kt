package encore.backstage.command

import encore.backstage.command.types.Argument
import encore.backstage.command.types.ArgumentInputs
import encore.backstage.command.types.ArgumentType
import encore.backstage.command.types.CommandResult
import game.context.ServerContext

/**
 * An example implementation of command.
 * Does nothing meaningful beside introducing patterns and testing command invocation.
 */
class ExampleCommand : Command {
    override val id: String = "xxx-example"
    override val name: String = "Example"
    override val description: String = "This is merely a test command, does nothing actually."
    override val arguments: List<Argument> = listOf(
        Argument("a", "Argument A", "An example of required string argument", ArgumentType.String),
        Argument("b", "Argument B", "An example of required string argument", ArgumentType.String),
        Argument("c", "Argument C", "e.g., c=2 for uncaught exception, c=3 for execution failure", ArgumentType.Int, 1)
    )

    override suspend fun execute(serverContext: ServerContext, args: ArgumentInputs): CommandResult {
        val a = args.getString("a")
        val b = args.getString("b")
        val c = args.getInt("c")

        if (c == 2) throw Exception("Uncaught exception from c=2")
        if (c == 3) return CommandResult.ExecutionFailure("Execution failure from c=3")

        return CommandResult.Success("Executed 'test' command with args: a=$a, b=$b, c=$c")
    }
}
