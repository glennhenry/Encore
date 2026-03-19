package encoreTest.utils

import encore.startup.venue.EncoreFancamConfig
import encore.utils.JSON
import encore.utils.logging.Fancam
import encore.utils.logging.LOG_FILE_DIRECTORY
import encore.utils.logging.Level
import encore.utils.logging.OfficialFancam
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Only test the logger display, not actual code unit tests.
 *
 * use this to show color
 * ./gradlew test --tests "encoreTest.utils.TestLoggerDisplay.testLogger" --console=plain
 */
class TestLoggerDisplay {
    @Test
    fun `logger with color`() = runTest {
        Fancam.initialize(
            OfficialFancam(
                EncoreFancamConfig(
                    level = Level.Trace.name,
                    colorEnabled = true,
                    useBackgroundColor = true,
                    fileNamePadding = 25,
                    maxLineLength = 500,
                    maxFileSize = 5,
                    maxFileRotation = 5
                )
            )
        )

        logExample()
    }

    @Test
    fun `logger without color`() = runTest {
        Fancam.initialize(
            OfficialFancam(
                EncoreFancamConfig(
                    level = Level.Trace.name,
                    colorEnabled = false,
                    useBackgroundColor = true,
                    fileNamePadding = 25,
                    maxLineLength = 500,
                    maxFileSize = 5,
                    maxFileRotation = 5
                )
            )
        )

        logExample()
    }

    @Test
    fun `logger with foreground color`() = runTest {
        Fancam.initialize(
            OfficialFancam(
                EncoreFancamConfig(
                    level = Level.Trace.name,
                    colorEnabled = true,
                    useBackgroundColor = false,
                    fileNamePadding = 25,
                    maxLineLength = 500,
                    maxFileSize = 5,
                    maxFileRotation = 5
                )
            )
        )

        logExample()
    }

    @Test
    fun `test track event logging`() {
        JSON.initialize(Json { prettyPrint = true })
        Fancam.initialize(
            OfficialFancam(
                EncoreFancamConfig(
                    level = Level.Trace.name,
                    colorEnabled = true,
                    useBackgroundColor = false,
                    fileNamePadding = 25,
                    maxLineLength = 500,
                    maxFileSize = 5,
                    maxFileRotation = 5
                )
            )
        )

        val name = "TrackEventTest"
        val output = File(LOG_FILE_DIRECTORY, "$name.json")
        if (output.exists()) output.delete()

        Fancam.track(name)
            .playerId("pid12345")
            .username("PlayerABC")
            .data("buildingId", "bldId12345")
            .data("completeAt", 12345678)
            .note { "Test1" }
            .log(Level.Trace)

        Fancam.track(name)
            .playerId("pid12345")
            .username("PlayerABC")
            .data("buildingId", "bldId12345")
            .data("completeAt", 12345678)
            .note { "Test2" }
            .log(Level.Debug)

        Fancam.track(name)

            .playerId("pid12345")
            .username("PlayerABC")
            .data("buildingId", "bldId12345")
            .data("completeAt", 12345678)
            .note { "Test3" }
            .log(Level.Info)

        Fancam.track(name)
            .playerId("pid12345")
            .username("PlayerABC")
            .data("buildingId", "bldId12345")
            .data("completeAt", 12345678)
            .note { "Test4" }
            .log(Level.Warn)

        Fancam.track(name)
            .data("textOnly", true)
            .note { "Test5" }
            .log(Level.Error)

        Fancam.track(name)
            .data("playerId", "pid12345")
            .data("buildingId", "bldId12345")
            .data("completeAt", 12345678)
            .note { "Test6" }
            .record()

        Fancam.track(name)
            .data("someList", listOf("hello", "world", "kotlin", "ktor"))
            .note { "Test7" }
            .record()

        Fancam.track(name)
            .data("tripleObject", Triple("first", 2, listOf(1, 2, 3)))
            .note { "Test8" }
            .record()

        Fancam.track(name)
            .note { "Test9" }
            .data("someTypedObject", SomeTypedObject(x = "dsf", y = 12, z = listOf("12", "12")))
            .record()

        Fancam.track(name)
            .data("playerId", "12345")
            .playerId("playerId")
            .data("playerId", true)
            .data("multiplePlayerId", "")
            .record()
    }

    private fun logExample() {
        Fancam.trace("TestTag") { "This is an example of 'Fancam.trace' message with custom tag with custom tag (1)." }
        Fancam.trace { "This is an example of 'Fancam.trace' message (1)." }
        Fancam.debug("TestTag") { "This is an example of 'Fancam.debug' message with custom tag with custom tag (1)." }
        Fancam.debug { "This is an example of 'Fancam.debug' message (1)." }
        Fancam.info("TestTag") { "This is an example of 'Fancam.info' message with custom tag with custom tag (1)." }
        Fancam.info { "This is an example of 'Fancam.info' message (1)." }
        Fancam.warn("TestTag") { "This is an example of 'Fancam.warn' message with custom tag with custom tag (1)." }
        Fancam.warn { "This is an example of 'Fancam.warn' message (1)." }
        Fancam.error("TestTag") { "This is an example of 'Fancam.error' message with custom tag with custom tag (1)." }
        Fancam.error { "This is an example of 'Fancam.error' message (1)." }

        Fancam.track("TestTrack")
            .playerId("playerId123")
            .username("playerABC")
            .log(Level.Info, false)
    }
}

@Serializable
data class SomeTypedObject(
    val x: String = "xyz",
    val y: Int = 10,
    val z: List<String> = listOf("1", "2", "3")
)
