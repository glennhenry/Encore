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
        // expected file: FancamDisplayTest-1.log
        // this will kept appending to the file
        // file rotation is enabled automatically

        val fancam = OfficialFancam(EncoreFancamConfig())
        Fancam.initialize(fancam)

        Fancam.event(Level.Trace, "tag1")
            .message { "This is an example of a trace message" }
            .logToFileOnly("FancamDisplayTest")

        Fancam.event(Level.Debug, "")
            .message { "Another one is a debug message which doesn't have tag" }
            .logToFileOnly("FancamDisplayTest")

        Fancam.event(Level.Info, "tag2")
            .message { "Info is usually used to announce server updates to server operator" }
            .logToFileOnly("FancamDisplayTest")

        Fancam.event(Level.Warn, "longtag")
            .message { "Warn is for odd behavior. Anyway this would print into console and output to file." }
            .setFileTarget("FancamDisplayTest")
            .log()

        Fancam.event(Level.Error, "smalltag")
            .message { "This is an example of error with exception alos being outputted." }
            .setThrowable(Exception("Example exception", RuntimeException("cause of the example")))
            .logToFileOnly("FancamDisplayTest")

        Fancam.event(Level.Warn, "xiaoting")
            .message { "Xiaoting is so attractive. I keep thinking about her. I think I have fallen?" }
            .logToFileOnly("FancamDisplayTest")

        fancam.flush()
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
