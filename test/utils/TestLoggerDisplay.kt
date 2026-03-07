package utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import utils.logging.DataLogger
import utils.logging.LogLevel
import utils.logging.Logger
import utils.logging.TELEMETRY_DIRECTORY
import java.io.File
import kotlin.test.Test

/**
 * Only test the logger display, not actual code unit tests.
 *
 * use this to show color
 * ./gradlew test --tests "utils.TestLoggerDisplay" --console=rich
 */
class TestLoggerDisplay {
    @Test
    fun testLogger() = runTest {
        Logger.updateSettings {
            it.copy(colorizeLevelLabelOnly = false, useForegroundColor = false)
        }

        Logger.verbose("TestTag") { "This is an example of 'Logger.verbose' message with custom tag with custom tag (1)." }
        Logger.verbose { "This is an example of 'Logger.verbose' message (2)." }
        Logger.verbose { "This is an example of 'Logger.verbose' message (3)." }
        Logger.debug("TestTag") { "This is an example of 'Logger.debug' message with custom tag with custom tag (1)." }
        Logger.debug { "This is an example of 'Logger.debug' message (2)." }
        Logger.debug { "This is an example of 'Logger.debug' message (3)." }
        Logger.info("TestTag") { "This is an example of 'Logger.info' message with custom tag with custom tag (1)." }
        Logger.info { "This is an example of 'Logger.info' message (2)." }
        Logger.info { "This is an example of 'Logger.info' message (3)." }
        Logger.warn("TestTag") { "This is an example of 'Logger.warn' message with custom tag with custom tag (1)." }
        Logger.warn { "This is an example of 'Logger.warn' message (2)." }
        Logger.warn { "This is an example of 'Logger.warn' message (3)." }
        Logger.error("TestTag") { "This is an example of 'Logger.error' message with custom tag with custom tag (1)." }
        Logger.error { "This is an example of 'Logger.error' message (2)." }
        Logger.error { "This is an example of 'Logger.error' message (3)." }

        delay(200)
        Logger.updateSettings {
            it.copy(colorizeLevelLabelOnly = true, useForegroundColor = false)
        }

        Logger.verbose("TestTag") { "This is an example of 'Logger.verbose' message with custom tag (1)." }
        Logger.verbose { "This is an example of 'Logger.verbose' message (2)." }
        Logger.verbose { "This is an example of 'Logger.verbose' message (3)." }
        Logger.debug("TestTag") { "This is an example of 'Logger.debug' message with custom tag (1)." }
        Logger.debug { "This is an example of 'Logger.debug' message (2)." }
        Logger.debug { "This is an example of 'Logger.debug' message (3)." }
        Logger.info("TestTag") { "This is an example of 'Logger.info' message with custom tag (1)." }
        Logger.info { "This is an example of 'Logger.info' message (2)." }
        Logger.info { "This is an example of 'Logger.info' message (3)." }
        Logger.warn("TestTag") { "This is an example of 'Logger.warn' message with custom tag (1)." }
        Logger.warn { "This is an example of 'Logger.warn' message (2)." }
        Logger.warn { "This is an example of 'Logger.warn' message (3)." }
        Logger.error("TestTag") { "This is an example of 'Logger.error' message with custom tag (1)." }
        Logger.error { "This is an example of 'Logger.error' message (2)." }
        Logger.error { "This is an example of 'Logger.error' message (3)." }

        delay(200)
        Logger.updateSettings {
            it.copy(colorizeLevelLabelOnly = false, useForegroundColor = true)
        }

        Logger.verbose("TestTag") { "This is an example of 'Logger.verbose' message with custom tag (1)." }
        Logger.verbose { "This is an example of 'Logger.verbose' message (2)." }
        Logger.verbose { "This is an example of 'Logger.verbose' message (3)." }
        Logger.debug("TestTag") { "This is an example of 'Logger.debug' message with custom tag (1)." }
        Logger.debug { "This is an example of 'Logger.debug' message (2)." }
        Logger.debug { "This is an example of 'Logger.debug' message (3)." }
        Logger.info("TestTag") { "This is an example of 'Logger.info' message with custom tag (1)." }
        Logger.info { "This is an example of 'Logger.info' message (2)." }
        Logger.info { "This is an example of 'Logger.info' message (3)." }
        Logger.warn("TestTag") { "This is an example of 'Logger.warn' message with custom tag (1)." }
        Logger.warn { "This is an example of 'Logger.warn' message (2)." }
        Logger.warn { "This is an example of 'Logger.warn' message (3)." }
        Logger.error("TestTag") { "This is an example of 'Logger.error' message with custom tag (1)." }
        Logger.error { "This is an example of 'Logger.error' message (2)." }
        Logger.error { "This is an example of 'Logger.error' message (3)." }

        delay(200)
        Logger.updateSettings {
            it.copy(colorizeLevelLabelOnly = true, useForegroundColor = true)
        }

        Logger.verbose("TestTag") { "This is an example of 'Logger.verbose' message with custom tag (1)." }
        Logger.verbose { "This is an example of 'Logger.verbose' message (2)." }
        Logger.verbose { "This is an example of 'Logger.verbose' message (3)." }
        Logger.debug("TestTag") { "This is an example of 'Logger.debug' message with custom tag (1)." }
        Logger.debug { "This is an example of 'Logger.debug' message (2)." }
        Logger.debug { "This is an example of 'Logger.debug' message (3)." }
        Logger.info("TestTag") { "This is an example of 'Logger.info' message with custom tag (1)." }
        Logger.info { "This is an example of 'Logger.info' message (2)." }
        Logger.info { "This is an example of 'Logger.info' message (3)." }
        Logger.warn("TestTag") { "This is an example of 'Logger.warn' message with custom tag (1)." }
        Logger.warn { "This is an example of 'Logger.warn' message (2)." }
        Logger.warn { "This is an example of 'Logger.warn' message (3)." }
        Logger.error("TestTag") { "This is an example of 'Logger.error' message with custom tag (1)." }
        Logger.error { "This is an example of 'Logger.error' message (2)." }
        Logger.error { "This is an example of 'Logger.error' message (3)." }
    }

    @Test
    fun testDataLogger() {
        JSON.initialize(Json { prettyPrint = true })
        val name = "DataLoggerTest"
        val output = File(TELEMETRY_DIRECTORY, "$name.json")
        if (output.exists()) output.delete()

        DataLogger.event(name)
            .prefixText("Test1")
            .playerId("pid12345")
            .username("PlayerABC")
            .data("buildingId", "bldId12345")
            .data("completeAt", 12345678)
            .log(LogLevel.Verbose)

        DataLogger.event(name)
            .prefixText("Test2")
            .playerId("pid12345")
            .username("PlayerABC")
            .data("buildingId", "bldId12345")
            .data("completeAt", 12345678)
            .log(LogLevel.Debug)

        DataLogger.event(name)
            .prefixText("Test3")
            .playerId("pid12345")
            .username("PlayerABC")
            .data("buildingId", "bldId12345")
            .data("completeAt", 12345678)
            .log(LogLevel.Info)

        DataLogger.event(name)
            .prefixText("Test4")
            .playerId("pid12345")
            .username("PlayerABC")
            .data("buildingId", "bldId12345")
            .data("completeAt", 12345678)
            .log(LogLevel.Warn)

        DataLogger.event(name)
            .prefixText("Test5")
            .data("textOnly", true)
            .log(LogLevel.Error, textOnly = true)

        DataLogger.event(name)
            .prefixText("Test6")
            .data("playerId", "pid12345")
            .data("buildingId", "bldId12345")
            .data("completeAt", 12345678)
            .record()

        DataLogger.event(name)
            .prefixText("Test7")
            .data("someList", listOf("hello", "world", "kotlin", "ktor"))
            .record()

        DataLogger.event(name)
            .prefixText("Test8")
            .data("tripleObject", Triple("first", 2, listOf(1, 2, 3)))
            .record()

        DataLogger.event(name)
            .prefixText("Test9")
            .data("someTypedObject", SomeTypedObject(x = "dsf", y = 12, z = listOf("12", "12")))
            .record()

        DataLogger.event(name)
            .data("playerId", "12345")
            .playerId("playerId")
            .data("playerId", true)
            .data("multiplePlayerId", "")
            .record()
    }
}

@Serializable
data class SomeTypedObject(
    val x: String = "xyz",
    val y: Int = 10,
    val z: List<String> = listOf("1", "2", "3")
)
