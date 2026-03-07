package security

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import security.validation.FailStrategy
import security.validation.ValidationResult
import security.validation.ValidationScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class TestValidationScheme {
    @Test
    fun `test validation pass`() {
        val result = ValidationScheme("example") { "HelloWorld Ktor" }
            .require("Contains space") { contains(" ") }
            .require("Contains 'Ktor'") { contains("Ktor") }
            .validate()
        assertIs<ValidationResult.Passed>(result)
    }

    @Test
    fun `test validation suspended version using validate fails`() = runTest {
        val result = ValidationScheme("example") { "HelloWorld Ktor" }
            .requireSuspend("Contains space") {
                delay(1000)
                contains(" ")
            }
            .require("Contains 'Ktor'") { contains("Ktor") }
            .validate()
        assertIs<ValidationResult.Error>(result)
    }

    @Test
    fun `test validation suspended version using validateSuspend pass`() = runTest {
        val result = ValidationScheme("example") { "HelloWorld Ktor" }
            .requireSuspend("Contains space") {
                delay(1000)
                contains(" ")
            }
            .require("Contains 'Ktor'") { contains("Ktor") }
            .validateSuspend()
        assertIs<ValidationResult.Passed>(result)
    }

    @Test
    fun `test validation fail at non-last stage with default strategy`() {
        val result = ValidationScheme("example") { "Hello Ktor" }
            .require("Contains space") { contains(" ") }
            .require("Contains 'World' FAILED") { contains("World") }
            .require("Contains 'Ktor'") { contains("Ktor") }
            .validate()

        assertIs<ValidationResult.Failed>(result)
        assertNotNull(result.failStrategy)
        assertEquals(result.failStrategy, FailStrategy.Cancel)
    }

    @Test
    fun `test validation fail at non-last stage with specified strategy, message, and failedAtStage`() {
        val result = ValidationScheme("example") { "Hello Ktor" }
            .require("Contains space") { contains(" ") }
            .require(
                "Contains 'World' FAILED",
                failStrategy = FailStrategy.Disconnect,
                failReason = "The input string does not contain 'World'"
            ) { contains("World") }
            .require("Contains 'Ktor'") { contains("Ktor") }
            .validate()

        assertIs<ValidationResult.Failed>(result)
        assertNotNull(result.failStrategy)
        assertEquals(result.failStrategy, FailStrategy.Disconnect)
        assertEquals(result.failReason, "The input string does not contain 'World'")
        assertEquals(result.failedAtStage, "stage-1: Contains 'World' FAILED")
    }

    @Test
    fun `test validation throw error return error result`() {
        val result = ValidationScheme("example") { "Hello Ktor" }
            .require("Contains space") { contains(" ") }
            .require("Contains 'World' ERROR") { throw Exception() }
            .require("Contains 'Ktor'") { contains("Ktor") }
            .validate()

        assertIs<ValidationResult.Error>(result)
    }
}
