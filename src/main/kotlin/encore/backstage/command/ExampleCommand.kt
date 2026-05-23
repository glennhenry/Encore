package encore.backstage.command

import encore.backstage.command.types.ArgumentCollection
import encore.backstage.command.types.ArgumentDescriptor
import encore.backstage.command.types.CommandResult
import encore.backstage.command.types.CommandVariant
import encore.context.ServerContext

/**
 * An example implementation of command.
 * Does nothing meaningful beside introducing patterns and testing command invocation.
 */
class ExampleCommand : Command {
    override val commandId: String = "test"
    override val description: String = "This is merely a test command, does nothing. c=2 simulates uncaught exception, c=3 simulates failure"
    override val variants = listOf(
        CommandVariant(
            listOf(
                ArgumentDescriptor("a", "String", "example of required string type"),
                ArgumentDescriptor("b", "String", "example of required string type"),
            ),
        ),
        CommandVariant(
            listOf(
                ArgumentDescriptor("a", "String", "example of required string type"),
                ArgumentDescriptor("b", "String", "example of required string type"),
                ArgumentDescriptor("c", "Int", "example of optional number type (defaults 1)"),
            ),
        ),
    )

    override fun execute(serverContext: ServerContext, args: ArgumentCollection): CommandResult {
        val a = args.next() ?: return CommandResult.NotEnoughArgument("a is required")
        val b = args.next() ?: return CommandResult.NotEnoughArgument("b is required")

        val expectedC = args.next()
        val c = if (expectedC != null) {
            expectedC.toIntOrNull()
                ?: return CommandResult.InvalidArgumentType("c is supposed to be an Integer type, got: '$expectedC'")
        } else {
            1
        }

        if (c == 2) throw Exception("Some uncaught exception")
        if (c == 3) return CommandResult.ExecutionFailure("Some failed execution due to domain error")

        return CommandResult.Executed("Successfully execute 'test' command with args: a=$a, b=$b, c=$c")
    }
}
