package devtools

import com.mongodb.assertions.Assertions.assertTrue
import context.ServerContext
import devtools.command.core.ArgumentCollection
import devtools.command.core.ArgumentDescriptor
import devtools.command.core.Command
import devtools.command.core.CommandDispatcher
import devtools.command.core.CommandRequest
import devtools.command.core.CommandResult
import devtools.command.core.CommandVariant
import devtools.command.core.variantsAsString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.assertThrows
import utils.JSON
import utils.logging.TestLogger
import utils.randomString
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Command dispatcher test and example of command implementation [ExampleGiveCommand].
 *
 * Does not need to test invalid request since parser already validates that.
 */
class CommandDispatcherTest {
    private val context = ServerContext.fake()

    @BeforeTest
    fun setupJson() {
        JSON.initialize(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }

    @Test
    fun `testCommandDispatcher register normal success`() {
        val dispatcher = CommandDispatcher(TestLogger())
        dispatcher.init(context)

        dispatcher.register(createCommand("cmd1"))
        dispatcher.register(createCommand("cmd2"))
        dispatcher.register(createCommand("cmd3"))
        dispatcher.getAllRegisteredCommandsId().containsAll(listOf("cmd1", "cmd2", "cmd3"))
    }

    @Test
    fun `testCommandDispatcher register normal with variant success`() {
        val dispatcher = CommandDispatcher(TestLogger())
        dispatcher.init(context)

        dispatcher.register(createCommand("cmd1", generateVariants()))
        dispatcher.register(createCommand("cmd2", generateVariants()))
        dispatcher.register(createCommand("cmd3", generateVariants()))
        dispatcher.getAllRegisteredCommandsId().containsAll(listOf("cmd1", "cmd2", "cmd3"))
    }

    @Test
    fun `testCommandDispatcher register duplicate commandId success but warned`() {
        val logger = TestLogger()
        val dispatcher = CommandDispatcher(logger)
        dispatcher.init(context)

        val cmd2a = generateVariant(2)
        val cmd2b = generateVariant(3)

        dispatcher.register(createCommand("cmd1"))
        dispatcher.register(createCommand("cmd2", listOf(cmd2a)))
        dispatcher.register(createCommand("cmd2", listOf(cmd2b)))

        val call = logger.getLastWarnCalls(1)

        // ensure warned
        assertTrue(call.first().contains("has been registered before"))
        // ensure the first registered command get overwritten
        assertTrue(dispatcher.getAllVariantsOf("cmd2").contains(cmd2b))
    }

    @Test
    fun `testCommandDispatcher register duplicate variant failed 1 and throws`() {
        val logger = TestLogger()
        val dispatcher = CommandDispatcher(logger)
        dispatcher.init(context)

        val first = CommandVariant(
            listOf(
                ArgumentDescriptor("abc", "String", "descriptionA"),
                ArgumentDescriptor("def", "String", "descriptionB"),
            )
        )

        val variants = listOf(
            first,
            CommandVariant(
                listOf(
                    ArgumentDescriptor("abc", "String", "descriptionC"),
                    ArgumentDescriptor("def", "String", "descriptionD"),
                )
            ),
        )

        assertThrows<IllegalArgumentException> {
            dispatcher.register(createCommand("cmd1", variants))
        }
    }

    @Test
    fun `testCommandDispatcher register duplicate variant failed 2 and throws`() {
        val logger = TestLogger()
        val dispatcher = CommandDispatcher(logger)
        dispatcher.init(context)

        val variants = listOf(generateVariant(2), generateVariant(2))

        assertThrows<IllegalArgumentException> {
            dispatcher.register(createCommand("cmd1", variants))
        }
    }

    @Test
    fun `testCommandDispatcher register commandId blank 1 throws`() {
        val logger = TestLogger()
        val dispatcher = CommandDispatcher(logger)
        dispatcher.init(context)

        val variants = listOf(generateVariant(2))

        assertThrows<IllegalArgumentException> {
            dispatcher.register(createCommand("", variants))
        }
    }

    @Test
    fun `testCommandDispatcher register commandId blank 2 throws`() {
        val logger = TestLogger()
        val dispatcher = CommandDispatcher(logger)
        dispatcher.init(context)

        val variants = listOf(generateVariant(2))

        assertThrows<IllegalArgumentException> {
            dispatcher.register(createCommand("   ", variants))
        }
    }

    @Test
    fun `testCommandDispatcher register commandId has invalid character 1 throws`() {
        val logger = TestLogger()
        val dispatcher = CommandDispatcher(logger)
        dispatcher.init(context)

        val variants = listOf(generateVariant(2))

        assertThrows<IllegalArgumentException> {
            dispatcher.register(createCommand("@@@", variants))
        }
    }

    @Test
    fun `testCommandDispatcher register commandId has invalid character 2 throws`() {
        val logger = TestLogger()
        val dispatcher = CommandDispatcher(logger)
        dispatcher.init(context)

        val variants = listOf(generateVariant(2))

        assertThrows<IllegalArgumentException> {
            dispatcher.register(createCommand("  @@@ ", variants))
        }
    }

    @Test
    fun `testCommandDispatcher register commandId has acceptable character does not throws`() {
        val logger = TestLogger()
        val dispatcher = CommandDispatcher(logger)
        dispatcher.init(context)

        val variants = listOf(generateVariant(2))

        dispatcher.register(createCommand("give-ext", variants))
    }

    @Test
    fun `testCommandDispatcher register commandId has whitespace character success`() {
        val logger = TestLogger()
        val dispatcher = CommandDispatcher(logger)
        dispatcher.init(context)

        val variants = listOf(generateVariant(2))

        dispatcher.register(createCommand("   give-ext", variants))
        assertTrue(dispatcher.getAllRegisteredCommandsId().contains("give-ext"))
    }

    @Test
    fun `testCommandDispatcher register commandId duplicate name but different cases success`() {
        val logger = TestLogger()
        val dispatcher = CommandDispatcher(logger)
        dispatcher.init(context)

        val variants = listOf(generateVariant(2))

        dispatcher.register(createCommand("give-ext", variants))
        dispatcher.register(createCommand("giVe-Ext", variants))
        dispatcher.register(createCommand("GIVE-EXT", variants))

        assertTrue(dispatcher.getAllRegisteredCommandsId().containsAll(listOf("give-ext", "giVe-Ext", "GIVE-EXT")))
    }

    @Test
    fun `testCommandDispatcher handleCommand unregistered command returns command not found`() {
        val logger = TestLogger()
        val dispatcher = CommandDispatcher(logger)
        dispatcher.init(context)

        assertTrue(
            dispatcher.handleCommand(
                CommandRequest(
                    "cmd",
                    buildArgCollection {})
            ) is CommandResult.CommandNotFound
        )
    }

    @Test
    fun `testCommandDispatcher handleCommand normally 1 returns executed`() {
        val logger = TestLogger()
        val dispatcher = CommandDispatcher(logger)
        dispatcher.init(context)

        dispatcher.register(ExampleGiveCommand())
        assertTrue(
            dispatcher.handleCommand(
                CommandRequest(
                    "give",
                    buildArgCollection {
                        add("playerAbc")
                        add("water")
                    })
            ) is CommandResult.Executed
        )
    }

    @Test
    fun `testCommandDispatcher handleCommand normally 2 returns executed`() {
        val logger = TestLogger()
        val dispatcher = CommandDispatcher(logger)
        dispatcher.init(context)

        dispatcher.register(ExampleGiveCommand())
        assertTrue(
            dispatcher.handleCommand(
                CommandRequest(
                    "give",
                    buildArgCollection {
                        add("playerAbc")
                        add("water")
                        add("100")
                    })
            ) is CommandResult.Executed
        )
    }

    @Test
    fun `testCommandDispatcher handleCommand insufficient arguments returns not enough argument`() {
        val logger = TestLogger()
        val dispatcher = CommandDispatcher(logger)
        dispatcher.init(context)

        dispatcher.register(ExampleGiveCommand())
        assertTrue(
            dispatcher.handleCommand(
                CommandRequest(
                    "give",
                    buildArgCollection {
                        add("playerABC")
                    })
            ) is CommandResult.NotEnoughArgument
        )
    }

    @Test
    fun `testCommandDispatcher handleCommand argument type mismatch returns invalid argument type`() {
        val logger = TestLogger()
        val dispatcher = CommandDispatcher(logger)
        dispatcher.init(context)

        dispatcher.register(ExampleGiveCommand())
        assertTrue(
            dispatcher.handleCommand(
                CommandRequest(
                    "give",
                    buildArgCollection {
                        add("playerABC")
                        add("water")
                        add("notNumber")
                    })
            ) is CommandResult.InvalidArgumentType
        )
    }

    @Test
    fun `testCommandDispatcher handleCommand simulates uncaught exception returns command error`() {
        val logger = TestLogger()
        val dispatcher = CommandDispatcher(logger)
        dispatcher.init(context)

        dispatcher.register(ExampleGiveCommand())
        assertTrue(
            dispatcher.handleCommand(
                CommandRequest(
                    "give",
                    buildArgCollection {
                        add("playerABC")
                        add("water")
                        add("2")
                    })
            ) is CommandResult.Error
        )
    }

    @Test
    fun `testCommandDispatcher handleCommand simulates failure returns execution failure`() {
        val logger = TestLogger()
        val dispatcher = CommandDispatcher(logger)
        dispatcher.init(context)

        dispatcher.register(ExampleGiveCommand())
        assertTrue(
            dispatcher.handleCommand(
                CommandRequest(
                    "give",
                    buildArgCollection {
                        add("playerABC")
                        add("water")
                        add("3")
                    })
            ) is CommandResult.ExecutionFailure
        )
    }

    @Test
    fun `testCommandDispatcher handleCommand too many arguments still success returns executed`() {
        val logger = TestLogger()
        val dispatcher = CommandDispatcher(logger)
        dispatcher.init(context)
        dispatcher.init(context)

        dispatcher.register(ExampleGiveCommand())
        assertTrue(
            dispatcher.handleCommand(
                CommandRequest(
                    "give",
                    buildArgCollection {
                        add("playerABC")
                        add("water")
                        add("4")
                        add("4")
                        add("a")
                        add("b")
                    })
            ) is CommandResult.Executed
        )
    }

    private val charPool = ('a'..'z') + ('A'..'Z')
    private val typePool = setOf("String", "Int", "Boolean", "Double")

    private fun generateDescriptor(): ArgumentDescriptor {
        val id = randomString(Random.nextInt(4, 9), charPool)
        val desc = randomString(Random.nextInt(10, 30), charPool)
        val type = typePool.random()

        return ArgumentDescriptor(id, type, desc)
    }

    private fun generateDescriptors(): List<ArgumentDescriptor> {
        return List(Random.nextInt(1, 4)) {
            generateDescriptor()
        }
    }

    private fun generateVariant(length: Int): CommandVariant {
        return CommandVariant(List(length) {
            generateDescriptor()
        })
    }

    private fun generateVariants(): List<CommandVariant> {
        return List(Random.nextInt(1, 4)) { idx ->
            generateVariant(length = idx)
        }
    }

    private fun createCommand(id: String, variants: List<CommandVariant> = emptyList()): Command {
        return object : Command {
            override val commandId: String = id
            override val description: String = "TestCommand of $id with variants: ${variants.variantsAsString()}"
            override val variants: List<CommandVariant> = variants

            override fun execute(serverContext: ServerContext, args: ArgumentCollection): CommandResult {
                return CommandResult.Executed("execute() called on command ($id)")
            }
        }
    }

    private fun buildArgCollection(block: MutableList<String>.() -> Unit): ArgumentCollection {
        val list = buildList(block)
        return ArgumentCollection(list)
    }
}
