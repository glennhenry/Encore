package devtools.command.impl

import context.ServerContext
import devtools.command.core.ArgumentCollection
import devtools.command.core.ArgumentDescriptor
import devtools.command.core.Command
import devtools.command.core.CommandResult
import devtools.command.core.CommandVariant
import kotlin.text.toIntOrNull

class ExampleCommand : Command {
    override val commandId: String = "test"
    override val description: String = "This is merely a test command, does nothing. arg3=2 simulates uncaught exception, arg3=3 simulates failure"
    override val variants = listOf(
        CommandVariant(
            listOf(
                ArgumentDescriptor("arg1", "String", "example of string type"),
                ArgumentDescriptor("arg2", "String", "example of string type"),
            ),
        ),
        CommandVariant(
            listOf(
                ArgumentDescriptor("arg1", "String", "example of string type"),
                ArgumentDescriptor("arg2", "String", "example of string type"),
                ArgumentDescriptor("arg3", "Int", "example of number type"),
            ),
        ),
    )

    override fun execute(serverContext: ServerContext, args: ArgumentCollection): CommandResult {
        val arg1 = args.next() ?: return CommandResult.NotEnoughArgument("arg1 is required")
        val arg2 = args.next() ?: return CommandResult.NotEnoughArgument("arg2 is required")

        val expectedArg3 = args.next()
        val arg3 = if (expectedArg3 != null) {
            expectedArg3.toIntOrNull()
                ?: return CommandResult.InvalidArgumentType("arg3 is supposed to be an Integer type, got: '$expectedArg3'")
        } else {
            1
        }

        if (arg3 == 2) throw Exception("Some uncaught exception")
        if (arg3 == 3) return CommandResult.ExecutionFailure("Failed to execute due to domain error")

        return CommandResult.Executed("Successfully execute test command with arg: $arg1, $arg2, $arg3")
    }
}
