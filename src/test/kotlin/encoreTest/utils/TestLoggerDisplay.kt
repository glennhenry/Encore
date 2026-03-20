package encoreTest.utils

import encore.annotation.RevisitLater
import encore.startup.venue.EncoreFancamConfig
import encore.utils.logging.Fancam
import encore.utils.logging.Level
import encore.utils.logging.OfficialFancam
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * Only test the logger display, not actual code unit tests.
 *
 * use this to show color
 * ./gradlew test --tests "encoreTest.utils.TestLoggerDisplay" --console=plain
 */
class TestLoggerDisplay {
    @Test
    fun `logger with color`() = runTest {
        Fancam.initialize(OfficialFancam(EncoreFancamConfig()))
        logExample()
    }

    @Test
    fun `logger without color`() = runTest {
        Fancam.initialize(OfficialFancam(EncoreFancamConfig(colorEnabled = false,)))
        logExample()
    }

    @Test
    fun `logger with foreground color`() = runTest {
        Fancam.initialize(OfficialFancam(EncoreFancamConfig(useBackgroundColor = false,)))
        logExample()
    }

    @RevisitLater("Unit tests could never produce any text to file. Track event formatter always return empty text")
    @Test
    fun `test track event logging`() = runTest {
        Fancam.initialize(OfficialFancam(EncoreFancamConfig()))

        val name = "TrackEventTest"

        // Track event
        Fancam.track(name)
            .data("playerId", "pid12345")
            .data("buildingId", "bldId12345")
            .data("completeAt", 12345678)
            .note { "Test6" }
            .route("TrackEventTest")
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
