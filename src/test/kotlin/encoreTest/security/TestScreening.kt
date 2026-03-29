package encoreTest.security

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import encore.security.screening.FailStrategy
import encore.security.screening.ScreeningResult
import encore.security.screening.Screening
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class TestScreening {
    @Test
    fun `test validation pass`() {
        val result = Screening("encore/exampleexample") { "HelloWorld Ktor" }
            .check("Contains space") { contains(" ") }
            .check("Contains 'Ktor'") { contains("Ktor") }
            .finalize()
        assertIs<ScreeningResult.Passed>(result)
    }

    @Test
    fun `test validation suspended version using validate fails`() = runTest {
        val result = Screening("encore/exampleexample") { "HelloWorld Ktor" }
            .checkSuspend("Contains space") {
                delay(1000)
                contains(" ")
            }
            .check("Contains 'Ktor'") { contains("Ktor") }
            .finalize()
        assertIs<ScreeningResult.Error>(result)
    }

    @Test
    fun `test validation suspended version using validateSuspend pass`() = runTest {
        val result = Screening("encore/exampleexample") { "HelloWorld Ktor" }
            .checkSuspend("Contains space") {
                delay(1000)
                contains(" ")
            }
            .check("Contains 'Ktor'") { contains("Ktor") }
            .finalizeSuspend()
        assertIs<ScreeningResult.Passed>(result)
    }

    @Test
    fun `test validation fail at non-last stage with default strategy`() {
        val result = Screening("encore/exampleexample") { "Hello Ktor" }
            .check("Contains space") { contains(" ") }
            .check("Contains 'World' FAILED") { contains("World") }
            .check("Contains 'Ktor'") { contains("Ktor") }
            .finalize()

        assertIs<ScreeningResult.Failed>(result)
        assertNotNull(result.failStrategy)
        assertEquals(result.failStrategy, FailStrategy.Cancel)
    }

    @Test
    fun `test validation fail at non-last stage with specified strategy, message, and failedAtStage`() {
        val result = Screening("encore/exampleexample") { "Hello Ktor" }
            .check("Contains space") { contains(" ") }
            .check(
                "Contains 'World' FAILED",
                failStrategy = FailStrategy.Disconnect,
                failReason = "The input string does not contain 'World'"
            ) { contains("World") }
            .check("Contains 'Ktor'") { contains("Ktor") }
            .finalize()

        assertIs<ScreeningResult.Failed>(result)
        assertNotNull(result.failStrategy)
        assertEquals(result.failStrategy, FailStrategy.Disconnect)
        assertEquals(result.failReason, "The input string does not contain 'World'")
        assertEquals(result.failedAtStage, "stage-1: Contains 'World' FAILED")
    }

    @Test
    fun `test validation throw error return error result`() {
        val result = Screening("encore/exampleexample") { "Hello Ktor" }
            .check("Contains space") { contains(" ") }
            .check("Contains 'World' ERROR") { throw Exception() }
            .check("Contains 'Ktor'") { contains("Ktor") }
            .finalize()

        assertIs<ScreeningResult.Error>(result)
    }
}
