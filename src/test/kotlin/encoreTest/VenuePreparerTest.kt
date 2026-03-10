package encoreTest

import encore.annotation.RevisitLater
import encore.annotation.VenueKey
import encore.startup.venue.VenuePreparer
import encoreTest.utils.toFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import encoreTest.utils.assertDoesNotFail

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

    // will throw if happen in one file
    @RevisitLater("Use test logger and assert warning")
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

        val preparer = VenuePreparer(listOf(xml1, xml2), "root")
        val config1 = preparer.get(TestConfig::class, "server")
        val config2 = preparer.get(TestConfig::class, "server")
        preparer.validate()

        assertEquals("localhost", config1.host)
        assertEquals("localhost", config2.host)
        assertEquals(7777, config2.port)
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

    @RevisitLater("Should not throw, but should warn internally")
    @Test
    fun unusedKeysDetected() {
        val xml = """
            <root>
                <server>
                    <host>localhost</host>
                    <port>8080</port>
                    <extra>ignored</extra>
                </server>
            </root>
        """.trimIndent().toFile("root.xml")

        val preparer = VenuePreparer(listOf(xml), "root")
        preparer.get(TestConfig::class, "server")

        assertDoesNotFail {
            preparer.validate()
        }
    }

    @Test
    fun missingAnnotationThrows() {
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
    fun missingConfigValueThrows() {
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
