package devtools

import context.ServerContext
import devtools.command.core.ArgumentCollection
import devtools.command.core.ArgumentDescriptor
import devtools.command.core.Command
import devtools.command.core.CommandResult
import devtools.command.core.CommandVariant
import kotlin.text.toIntOrNull

/**
 * Example of how command can be implemented.
 *
 * In this case, a 'give' command where the server gives a
 * specific item to a particular player for an amount.
 */
class ExampleGiveCommand : Command {
    override val commandId: String = "give"
    override val description: String = "Give a particular item of an amount to a specific player."
    override val variants = listOf(
        // give playerId itemId
        CommandVariant(
            listOf(
                ArgumentDescriptor("playerId", "String", "the target playerId"),
                ArgumentDescriptor("itemId", "String", "the ID of item to be given"),
            ),
        ),
        // give playerId itemId 100
        CommandVariant(
            listOf(
                ArgumentDescriptor("playerId", "String", "the target playerId"),
                ArgumentDescriptor("itemId", "String", "the ID of item to be given"),
                ArgumentDescriptor("amount", "Int", "amount of item to be sent"),
            ),
        ),
    )

    /**
     * amount = 2 simulates uncaught exception, amount = 3 simulates failure
     */
    override fun execute(serverContext: ServerContext, args: ArgumentCollection): CommandResult {
        val playerId = args.next() ?: return CommandResult.NotEnoughArgument("playerId is required")
        val itemId = args.next() ?: return CommandResult.NotEnoughArgument("itemId is required")

        val expectedAmount = args.next()
        val amount = if (expectedAmount != null) {
            expectedAmount.toIntOrNull()
                ?: return CommandResult.InvalidArgumentType("amount is supposed to be an Integer type, got: $expectedAmount")
        } else {
            1
        }

        if (amount == 2) throw Exception()
        if (amount == 3) return CommandResult.ExecutionFailure("Failed to execute")

        return CommandResult.Executed("Successfully give $amount $itemId to $playerId.")
    }
}
