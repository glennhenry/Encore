package encoreTest

import encore.annotation.VenueKey
import encore.startup.venue.EncoreConfig
import encore.startup.venue.FakeEnvProvider
import encore.startup.venue.VenuePreparer
import encore.utils.logging.Logger
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
            <venue>
                <encore>
                    <server>
                        <host>localhost</host>
                        <port>8080</port>
                    </server>
                </encore>
            </venue>
        """.trimIndent().toFile("venue.xml")

        val preparer = VenuePreparer(listOf(xml))
        val config = preparer.get(TestConfig::class, "encore")

        assertEquals("localhost", config.server.host)
        assertEquals(8080, config.server.port)
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
                <custom>
                    <parent>
                        <child>123</child>
                    </parent>
                </custom>
            </venue>
        """.trimIndent().toFile("venue.xml")

        val preparer = VenuePreparer(listOf(xml))
        val config = preparer.get(EncoreConfig::class, "encore")
        val custom = preparer.get(TestCustomConfig::class, "custom")

        Logger.info { config.toString() }
        Logger.info { custom.toString() }

        assertEquals(true, config.devMode)
        assertEquals("localhost", config.server.host)
        assertEquals(21, config.logger.fileNamePadding)
        assertEquals("heheDB", config.database.dbNameTest)
        assertEquals("ABCDEF", config.logger.timestampFormat)
        assertEquals(123, custom.child)
    }

    @Test
    fun `XML normal behavior real data with secret success`() {
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
                <random>
                    <parent>
                        <child>123</child>
                    </parent>
                </random>
            </venue>
        """.trimIndent().toFile("venue.xml")
        val xml2 = """
            <venue>
                <secret>
                    <anything>123</anything>
                </secret>
            </venue>
        """.trimIndent().toFile("venue2.xml")

        val preparer = VenuePreparer(listOf(xml, xml2))
        val secret = preparer.get(TestSecretConfig::class, "secret")

        Logger.info { secret.toString() }

        assertEquals(123, secret.anything)
    }

    @Test
    fun `XML normal behavior real data with ENV override success`() {
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

        val envProvider = FakeEnvProvider(
            mapOf(
                "ENCORE_LOGGER_COLOR__ENABLED" to "false",
                "ENCORE_DATABASE_MONGO_PROD_DBNAME" to "testdbname",
            )
        )
        val preparer = VenuePreparer(listOf(xml), envProvider)
        val encore = preparer.get(EncoreConfig::class, "encore")

        assertEquals(false, encore.logger.colorEnabled)
        assertEquals("testdbname", encore.database.dbNameProd)
    }

    @Test
    fun `XML normal behavior 2 files success`() {
        val xml1 = """
            <venue>
                <encore>
                    <server>
                        <host>localhost</host>
                        <port>8080</port>
                    </server>
                </encore>
            </venue>
        """.trimIndent().toFile("venue1.xml")
        val xml2 = """
            <venue>
                <secret>
                    <anything>123</anything>
                </secret>
            </venue>
        """.trimIndent().toFile("venue2.xml")

        val preparer = VenuePreparer(listOf(xml1, xml2))
        val config1 = preparer.get(TestConfig::class, "encore")
        val config2 = preparer.get(TestSecretConfig::class, "secret")

        assertEquals("localhost", config1.server.host)
        assertEquals(8080, config1.server.port)
        assertEquals(123, config2.anything)
    }

    // must between 2 files, it will throw if happen in one file
    @Test
    fun `XML duplicate keys between 2 files success but warned`() {
        val xml1 = """
            <venue>
                <encore>
                    <server>
                        <host>localhost</host>
                        <port>8080</port>
                    </server>
                </encore>
            </venue>
        """.trimIndent().toFile("venue1.xml")
        val xml2 = """
            <venue>
                <encore>
                    <server>
                        <host>localhost</host>
                        <port>7777</port>
                    </server>
                </encore>
            </venue>
        """.trimIndent().toFile("venue2.xml")

        val logger = TestLogger()
        val preparer = VenuePreparer(listOf(xml1, xml2), logger = logger)
        val config = preparer.get(TestConfig::class, "encore")

        assertEquals("localhost", config.server.host)
        assertEquals(7777, config.server.port)

        preparer.validate()

        val warns = logger.getLastWarnCalls(2)

        assertTrue {
            warns.first().contains(
                "Duplicate configuration key detected: 'venue.encore.server.host'. Last value wins localhost."
            )
        }

        assertTrue {
            warns.last().contains(
                "Duplicate configuration key detected: 'venue.encore.server.port'. Last value wins 7777."
            )
        }
    }

    @Test
    fun `XML no config value but config definition has default success`() {
        val xml = """
            <venue>
                <encore>
                    <host>localhost</host>
                </encore>
            </venue>
        """.trimIndent().toFile("venue.xml")

        val preparer = VenuePreparer(listOf(xml))
        val config = preparer.get(TestConfigWithDefault::class, "encore")

        assertEquals("localhost", config.host)
        assertEquals(7777, config.port)
    }

    @Test
    fun `XML has custom config that is not defined but have default`() {
        val xml = """
            <venue>
                <encore>
                </encore>
                <custom>
                    <field1>1</field1>
                </custom>
            </venue>
        """.trimIndent().toFile("venue.xml")

        val preparer = VenuePreparer(listOf(xml))
        val custom = preparer.get(TestCustomConfigWithDefault::class, "custom")

        assertEquals(2, custom.field2)
    }

    @Test
    fun `XML has invalid type should throws`() {
        val xml = """
            <venue>
                <encore>
                    <server>
                        <host>localhost</host>
                        <port>abc</port>
                    </server>
                </encore>
            </venue>
        """.trimIndent().toFile("venue.xml")

        val preparer = VenuePreparer(listOf(xml))

        assertFailsWith(NumberFormatException::class) {
            preparer.get(TestConfig::class, "encore")
        }
    }

    @Test
    fun `XML has unused keys success but get warned`() {
        val xml = """
            <venue>
                <encore>
                    <server>
                        <host>localhost</host>
                        <port>8080</port>
                        <extra>ignored</extra>
                    </server>
                </encore>
            </venue>
        """.trimIndent().toFile("venue.xml")

        val logger = TestLogger()
        val preparer = VenuePreparer(listOf(xml), logger = logger)
        preparer.get(TestConfig::class, "encore")

        assertDoesNotFail {
            preparer.validate()
        }
        logger.getLastWarnCalls(1).contains("Unused configuration keys detected")
    }

    @Test
    fun `config data class missing annotation`() {
        val xml = """
            <venue>
                <encore>
                    <server>
                        <host>localhost</host>
                    </server>
                </encore>
            </venue>
        """.trimIndent().toFile("venue.xml")

        val preparer = VenuePreparer(listOf(xml))

        assertFailsWith<IllegalStateException> {
            preparer.get(MissingAnnotationConfig::class, "encore")
        }
    }

    @Test
    fun `XML missing a configuration value from data class definition`() {
        val xml = """
            <venue>
                <encore>
                    <server>
                        <host>localhost</host>
                    </server>
                </encore>
            </venue>
        """.trimIndent().toFile("venue.xml")

        val preparer = VenuePreparer(listOf(xml))
        assertFailsWith<IllegalStateException> {
            preparer.get(TestConfig::class, "encore")
        }
    }

    @Test
    fun `XML defines structure incorrectly by mismatch with given category`() {
        val xml = """
            <venue>
                <notencore>
                    <server>
                        <host>localhost</host>
                        <port>8080</port>
                    </server>
                </notencore>
            </venue>
        """.trimIndent().toFile("venue.xml")

        val preparer = VenuePreparer(listOf(xml))
        assertFailsWith<IllegalStateException> {
            preparer.get(TestConfig::class, "encore")
        }
    }

    @Test
    fun `XML defines structure correctly with a custom category tag`() {
        val xml = """
            <venue>
                <notencore>
                    <server>
                        <host>localhost</host>
                        <port>8080</port>
                    </server>
                </notencore>
            </venue>
        """.trimIndent().toFile("venue.xml")

        val preparer = VenuePreparer(listOf(xml))
        assertDoesNotFail {
            preparer.get(TestConfig::class, "notencore")
        }
    }

    /**
     * Note: Unexpected behavior happen when defining ENV for XML
     * that has no `encore` tag such as below, where the first
     * parent tag of the field will be swallowed.
     */
    @Test
    fun `XML defines structure incorrectly by mismatch with given category without encore tag`() {
        val xml = """
            <venue>
                <server2>
                    <host>localhost</host>
                    <port>8080</port>
                </server2>
            </venue>
        """.trimIndent().toFile("venue.xml")

        val preparer = VenuePreparer(listOf(xml))
        assertFailsWith<IllegalStateException> {
            preparer.get(TestConfig::class, "server")
        }
    }

    @Test
    fun `XML have value but also defined in ENV for override`() {
        val xml = """
            <venue>
                <encore>
                    <server>
                        <host>localhost</host>
                        <port>8080</port>
                    </server>
                </encore>
            </venue>
        """.trimIndent().toFile("venue.xml")

        val envProvider = FakeEnvProvider(
            mapOf("ENCORE_SERVER_HOST" to "127.0.0.1")
        )
        val preparer = VenuePreparer(listOf(xml), envProvider)
        val config = preparer.get(TestConfig::class, "encore")
        assertEquals("127.0.0.1", config.server.host)
    }

    @Test
    fun `XML does not have value but defined in ENV`() {
        val xml = """
            <venue>
                <encore>
                    <server>
                        <port>7777</port>
                    </server>
                </encore>
            </venue>
        """.trimIndent().toFile("venue.xml")

        val envProvider = FakeEnvProvider(
            mapOf("ENCORE_SERVER_HOST" to "localhost")
        )
        val preparer = VenuePreparer(listOf(xml), envProvider)
        val config = preparer.get(TestConfig::class, "encore")
        assertEquals("localhost", config.server.host)
    }

    @Test
    fun `XML have value but also defined in ENV for custom config`() {
        val xml = """
            <venue>
                <encore>
                    <server>
                        <host>localhost</host>
                        <port>8080</port>
                    </server>
                </encore>
                <custom>
                    <parent>
                        <child>123</child>
                    </parent>
                </custom>
            </venue>
        """.trimIndent().toFile("venue.xml")

        val envProvider = FakeEnvProvider(
            mapOf("ENCORE_PARENT_CHILD" to "1234")
        )
        val preparer = VenuePreparer(listOf(xml), envProvider)
        val custom = preparer.get(TestCustomConfig::class, "encore")
        assertEquals(1234, custom.child)
    }

    @Test
    fun `XML does not have value but defined in ENV with different type fail`() {
        val xml = """
            <venue>
                <encore>
                    <server>
                        <host>localhost</host>
                    </server>
                </encore>
            </venue>
        """.trimIndent().toFile("venue.xml")

        val envProvider = FakeEnvProvider(
            mapOf("ENCORE_SERVER_PORT" to "notnumber")
        )
        val preparer = VenuePreparer(listOf(xml), envProvider)
        assertFailsWith<NumberFormatException> {
            preparer.get(TestConfig::class, "encore")
        }
    }
}

data class TestConfig(
    val server: TestConfigServer
)

data class TestConfigServer(
    @VenueKey("server.host")
    val host: String,

    @VenueKey("server.port")
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

data class TestCustomConfig(
    @VenueKey("parent.child")
    val child: Int
)

data class TestCustomConfigWithDefault(
    @VenueKey("field1")
    val field1: Int,

    @VenueKey("field2")
    val field2: Int = 2
)
