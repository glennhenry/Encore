package encoreTest.fancam

import encore.annotation.source.RevisitLater
import encore.EncoreFancamConfig
import encore.fancam.Fancam
import encore.fancam.events.Level
import encore.fancam.impl.OfficialFancam
import kotlinx.coroutines.test.runTest
import testHelper.toTempFile
import java.io.File
import kotlin.test.Test

/**
 * Only test the logger display, not actual code unit tests.
 *
 * use this to show color
 * ./gradlew test --tests "encoreTest.fancam.FancamDisplayTest" --console=plain
 */
class FancamDisplayTest {
    @Test
    fun `fancam without color`() = runTest {
        Fancam.initialize(OfficialFancam(EncoreFancamConfig(colorEnabled = false)))
        logExample()
    }

    @Test
    fun `fancam with foreground color`() = runTest {
        Fancam.initialize(OfficialFancam(EncoreFancamConfig(useBackgroundColor = false)))
        logExample()
    }

    @Test
    fun `fancam with color`() = runTest {
        Fancam.initialize(OfficialFancam(EncoreFancamConfig()))
        logExample()
    }

    @Test
    fun `fancam route to file`() = runTest {
        Fancam.initialize(OfficialFancam(EncoreFancamConfig()))

        Fancam.event(Level.Trace, "trace")
            .message { "Trace message" }
            .logToFileOnly("FancamDisplayTest")

        Fancam.event(Level.Debug, "")
            .message { "Debug message" }
            .logToFileOnly("FancamDisplayTest")

        Fancam.event(Level.Info, "info")
            .message { "Info message" }
            .logToFileOnly("FancamDisplayTest")

        Fancam.event(Level.Warn, "warn")
            .message { "Warn message" }
            .logToFileOnly("FancamDisplayTest")

        Fancam.event(Level.Error, "error")
            .message { "Error message" }
            .logToFileOnly("FancamDisplayTest")

        Fancam.event(Level.Warn, "warn")
            .message { "Warn message" }
            .logToFileOnly("FancamDisplayTest")
    }

    @RevisitLater("Unit tests could never produce any text to file. Track event formatter always return empty text")
    @Test
    fun `fancam track event`() = runTest {
        val fancam = OfficialFancam(EncoreFancamConfig())
        Fancam.initialize(fancam)
        Fancam.track("TrackEventTest")
            .data("playerId", "pid12345")
            .data("completeAt", 12345678)
            .data("Triple", Triple("s", "s", "s"))
            .data("Liz", listOf("Liz"))
            .data("Complex data", EncoreFancamConfig())
            .note { "This is a test track" }
            .route("TrackEventTest")
            .log(level = Level.Info)
        fancam.flush()
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
        Fancam.error(
            Exception("Example exception", RuntimeException("cause of the example")),
            "TestTag"
        ) { "This is an example of 'Fancam.error' message with custom tag with custom tag (1)." }
        Fancam.error { "This is an example of 'Fancam.error' message (1)." }
    }
}
