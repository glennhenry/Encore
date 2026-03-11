package encoreTest

import encore.annotation.VenueKey
import encore.startup.venue.EncoreConfig
import encore.startup.venue.FakeEnvProvider
import encore.startup.venue.VenuePreparer
import encore.utils.logging.TestLogger
import encoreTest.utils.toFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import encoreTest.utils.assertDoesNotFail
import kotlin.test.assertTrue

class VenuePreparerTest {
    @Test
    fun `XML normal behavior success`() {
        val xml = """
            <root>
                <server>
                    <host>localhost</host>
                    <port>8080</port>
                </server>
            </root>
        """.trimIndent().toFile("root.xml")

        val preparer = VenuePreparer(listOf(xml), "root")
        val config = preparer.get(TestConfig::class, "server")
        preparer.validate()

        assertEquals("localhost", config.host)
        assertEquals(8080, config.port)
    }

    @Test
    fun `XML normal behavior real data success`() {
        val xml = """
            <venue>
                <encore>
                    <devMode>true</devMode>
                    <server>
                        <host>localhost</host>
                        <port>8080</port>
                        <socketPort>7777</socketPort>
                    </server>
                    <admin enabled="true" />
                    <database>
                        <mongo>
                            <prod>
                                <dbname>CHANGE_ME-prod-DB</dbname>
                                <dburl>mongodb://localhost:27017</dburl>
                            </prod>
                            <test>
                                <dbname>heheDB</dbname>
                                <dburl>mongodb://localhost:27017</dburl>
                            </test>
                        </mongo>
                    </database>
                    <logger>
                        <level>TRACE</level>
                        <color enabled="true">
                            <colorEntireMessage>true</colorEntireMessage>
                            <useBackgroundColor>true</useBackgroundColor>
                        </color>
                        <formatting>
                            <fileNamePadding>21</fileNamePadding>
                            <maximumLineLength>500</maximumLineLength>
                            <maxFileSize>5</maxFileSize>
                            <maxFileRotation>5</maxFileRotation>
                            <timestampFormat>ABCDEF</timestampFormat>
                        </formatting>
                    </logger>
                </encore>
            </venue>
        """.trimIndent().toFile("venue.xml")

        val preparer = VenuePreparer(listOf(xml), "venue")
        val config = preparer.get(EncoreConfig::class, "encore")
        preparer.validate()

        println(config)

        assertEquals(true, config.devMode)
        assertEquals("localhost", config.server.host)
        assertEquals(21, config.logger.fileNamePadding)
        assertEquals("heheDB", config.database.dbNameTest)
        assertEquals("ABCDEF", config.logger.timestampFormat)
    }

    @Test
    fun `XML normal behavior 2 files success`() {
        val xml1 = """
            <root>
                <server>
                    <host>localhost</host>
                    <port>8080</port>
                </server>
            </root>
        """.trimIndent().toFile("root1.xml")
        val xml2 = """
            <root>
                <secret>
                    <anything>123</anything>
                </secret>
            </root>
        """.trimIndent().toFile("root2.xml")

        val preparer = VenuePreparer(listOf(xml1, xml2), "root")
        val config1 = preparer.get(TestConfig::class, "server")
        val config2 = preparer.get(TestSecretConfig::class, "secret")
        preparer.validate()

        assertEquals("localhost", config1.host)
        assertEquals(8080, config1.port)
        assertEquals(123, config2.anything)
    }

    // must between 2 files, it will throw if happen in one file
    @Test
    fun `XML duplicate keys between 2 files success but warned`() {
        val xml1 = """
            <root>
                <server>
                    <host>localhost</host>
                    <port>8080</port>
                </server>
            </root>
        """.trimIndent().toFile("root1.xml")
        val xml2 = """
            <root>
                <server>
                    <host>localhost</host>
                    <port>7777</port>
                </server>
            </root>
        """.trimIndent().toFile("root2.xml")

        val logger = TestLogger()
        val preparer = VenuePreparer(listOf(xml1, xml2), "root", logger = logger)
        val config = preparer.get(TestConfig::class, "server")

        assertEquals("localhost", config.host)
        assertEquals(7777, config.port)

        preparer.validate()

        val warns = logger.getLastWarnCalls(2)

        assertTrue {
            warns.first().contains(
                "Duplicate configuration key detected: 'root.server.host'. Last value wins localhost."
            )
        }

        assertTrue {
            warns.last().contains(
                "Duplicate configuration key detected: 'root.server.port'. Last value wins 7777."
            )
        }
    }

    @Test
    fun `XML no config value but config definition has default success`() {
        val xml = """
            <root>
                <server>
                    <host>localhost</host>
                </server>
            </root>
        """.trimIndent().toFile("root.xml")

        val preparer = VenuePreparer(listOf(xml), "root")
        val config = preparer.get(TestConfigWithDefault::class, "server")
        preparer.validate()

        assertEquals("localhost", config.host)
        assertEquals(7777, config.port)
    }

    @Test
    fun `XML has invalid type should throws`() {
        val xml = """
            <root>
                <server>
                    <host>localhost</host>
                    <port>abc</port>
                </server>
            </root>
        """.trimIndent().toFile("root.xml")

        val preparer = VenuePreparer(listOf(xml), "root")

        assertFailsWith(NumberFormatException::class) {
            preparer.get(TestConfig::class, "server")
        }
    }

    @Test
    fun `XML has unused keys success but get warned`() {
        val xml = """
            <root>
                <server>
                    <host>localhost</host>
                    <port>8080</port>
                    <extra>ignored</extra>
                </server>
            </root>
        """.trimIndent().toFile("root.xml")

        val logger = TestLogger()
        val preparer = VenuePreparer(listOf(xml), "root", logger = logger)
        preparer.get(TestConfig::class, "server")

        assertDoesNotFail {
            preparer.validate()
        }
        logger.getLastWarnCalls(1).contains("Unused configuration keys detected")
    }

    @Test
    fun `config data class missing annotation`() {
        val xml = """
            <root>
                <server>
                    <host>localhost</host>
                </server>
            </root>
        """.trimIndent().toFile("root.xml")

        val preparer = VenuePreparer(listOf(xml), "root")

        assertFailsWith<IllegalStateException> {
            preparer.get(MissingAnnotationConfig::class, "server")
        }
    }

    @Test
    fun `XML missing a configuration value from data class definition`() {
        val xml = """
            <root>
                <server>
                    <host>localhost</host>
                </server>
            </root>
        """.trimIndent().toFile("root.xml")

        val preparer = VenuePreparer(listOf(xml), "root")
        assertFailsWith<IllegalStateException> {
            preparer.get(TestConfig::class, "server")
        }
    }

    @Test
    fun `XML have value but overrided by ENV`() {
        val xml = """
            <root>
                <server>
                    <host>localhost</host>
                    <port>8080</port>
                </server>
            </root>
        """.trimIndent().toFile("root.xml")

        val envProvider = FakeEnvProvider(
            mapOf("ENCORE_SERVER_HOST" to "127.0.0.1")
        )
        val preparer = VenuePreparer(listOf(xml), "root", envProvider)
        val config = preparer.get(TestConfig::class, "server")
        assertEquals("127.0.0.1", config.host)
    }

    @Test
    fun `XML does not have value but defined in ENV`() {
        val xml = """
            <root>
                <server>
                    <port>7777</port>
                </server>
            </root>
        """.trimIndent().toFile("root.xml")

        val envProvider = FakeEnvProvider(
            mapOf("ENCORE_SERVER_HOST" to "localhost")
        )
        val preparer = VenuePreparer(listOf(xml), "root", envProvider)
        val config = preparer.get(TestConfig::class, "server")
        assertEquals("localhost", config.host)
    }

    @Test
    fun `XML does not have value but defined in ENV with different type fail`() {
        val xml = """
            <root>
                <server>
                    <host>localhost</host>
                </server>
            </root>
        """.trimIndent().toFile("root.xml")

        val envProvider = FakeEnvProvider(
            mapOf("ENCORE_SERVER_PORT" to "notnumber")
        )
        val preparer = VenuePreparer(listOf(xml), "root", envProvider)
        assertFailsWith<NumberFormatException> {
            preparer.get(TestConfig::class, "server")
        }
    }
}

data class TestConfig(
    @VenueKey("host")
    val host: String,

    @VenueKey("port")
    val port: Int
)

data class TestSecretConfig(
    @VenueKey("anything")
    val anything: Int
)

data class TestConfigWithDefault(
    @VenueKey("host")
    val host: String,

    @VenueKey("port")
    val port: Int = 7777
)

data class MissingAnnotationConfig(
    val host: String
)
