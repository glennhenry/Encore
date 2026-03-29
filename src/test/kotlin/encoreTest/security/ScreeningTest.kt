package encoreTest.security

import encore.EncoreFancamConfig
import encore.fancam.Fancam
import encore.fancam.impl.OfficialFancam
import encore.security.screening.Screening
import encore.security.screening.ScreeningResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import testHelper.assertDoesNotFail
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ScreeningTest {
    @BeforeTest
    fun setup() {
        Fancam.initialize(OfficialFancam(EncoreFancamConfig()))
    }
    @Test
    fun `screening pass`() {
        val result = Screening("ScreeningTest") { "HelloWorld Ktor" }
            .checkFor("Example")
            .check("Contains space") { contains(" ") }
            .check("Contains 'Ktor'") { contains("Ktor") }
            .finish()
        assertIs<ScreeningResult.Passed>(result)
    }

    @Test
    fun `screening fails at the correct stage index`() {
        val result = Screening("ScreeningTest") { "HelloWorld Ktor" }
            .check("Contains space") { contains(" ") }
            .check("Contains 'Kitty'") { contains("Kitty") }
            .check("Contains 'Ktor'") { contains("Ktor") }
            .finish()

        assertDoesNotFail {
            result as ScreeningResult.Failed
            assertEquals(2, result.stageIndex)
        }
    }

    @Test
    fun `screening includes suspended check with finishSuspend pass`() = runTest {
        val result = Screening("ScreeningTest") { "HelloWorld Ktor" }
            .checkSuspend("Contains space") {
                delay(1000)
                contains(" ")
            }
            .check("Contains 'Ktor'") { contains("Ktor") }
            .finishSuspend()
        assertIs<ScreeningResult.Passed>(result)
    }

    @Test
    fun `screening includes suspended check without finishSuspend error`() = runTest {
        val result = Screening("ScreeningTest") { "HelloWorld Ktor" }
            .checkSuspend("Contains space") {
                delay(1000)
                contains(" ")
            }
            .check("Contains 'Ktor'") { contains("Ktor") }
            .finish()
        assertIs<ScreeningResult.Error>(result)
    }

    @Test
    fun `screening error returns error result and correct stage index`() {
        val result = Screening("ScreeningTest") { "Hello Ktor" }
            .check("Contains space") { contains(" ") }
            .check("Contains 'Ktor'") { contains("Ktor") }
            .check("Contains 'World' ERROR") { throw Exception() }
            .finish()

        assertDoesNotFail {
            result as ScreeningResult.Error
            assertEquals(3, result.stageIndex)
        }
    }
}
