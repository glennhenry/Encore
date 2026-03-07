package example

import core.data.GameDefinition
import core.data.resources.GameResource
import core.data.resources.GameResourcesParser
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Demonstrate the way to test parser for a game definition resource.
 */
class ExampleDefinitionParserTest {
    /**
     * Parser has a high coupling with GameDefinition as it populates data directly.
     * Implementor must add members manually and parser have to know the name of the members.
     * Being a singleton object, it adds more complexity.
     *
     * `GameResourceParser` solely parse the given resource and populate GameDefinition manually.
     * The `parse` method doesn't return anything so it's quite hard to test.
     *
     * The way we can test GameDefinition is by mocking the object to have the
     * same members as the original parser use to operate.
     *
     * As for the GameResource, we can provide a temp file for them.
     */
    @Test
    fun testExampleResParser() {
        val tempResFile = File.createTempFile("test_res", ".res")
        tempResFile.writeText("""
            hello=1
            world=2
            kotlin=3
            ktor=4
        """.trimIndent())

        val res = ResResource(tempResFile.path)
        ExampleResParser().parse(res, GameDefinition)

        assertEquals(GameDefinition.exampleFromResParser["hello"], 1)
        assertEquals(GameDefinition.exampleFromResParser["ktor"], 4)
        assertEquals(GameDefinition.exampleFromResParser.size, 4)
    }
}

/**
 * Example of [GameResource] format.
 *
 * The file extension is ".res" and it simply reads the text without additional decompression.
 */
class ResResource(override val path: String) : GameResource {
    override val name: String = "ExampleRes"

    override fun readText(): String {
        return File(path).readText()
    }

    // not supported
    override fun readBytes(): ByteArray = byteArrayOf()
}

/**
 * Example of parser for the [ResResource] type of game resources.
 *
 * The `.res` format is just "<property>=<number>".
 */
class ExampleResParser : GameResourcesParser<ResResource> {
    private val map = mutableMapOf<String, Int>()
    override fun parse(res: ResResource, gameDefinition: GameDefinition) {
        val text = res.readText()
        for (line in text.lines()) {
            val group = line.split("=")
            if (group.size != 2) continue
            map[group[0]] = group[1].toInt()
        }
        gameDefinition.exampleFromResParser.putAll(map)
    }
}
