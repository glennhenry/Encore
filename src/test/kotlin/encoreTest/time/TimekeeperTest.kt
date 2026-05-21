package encoreTest.time

import encore.time.Timekeeper
import encore.time.source.MutableTimeSource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimekeeperTest {
    @Test
    fun `hasElapsedBy test`() {
        val timekeeper = Timekeeper(MutableTimeSource())

        val oldTime = timekeeper.now()
        // assuming mutable time source is correct...
        timekeeper.control.forwardBy(2.minutes)
        assertTrue(timekeeper.hasElapsedBy(oldTime, 1.minutes))
        assertFalse(timekeeper.hasElapsedBy(oldTime, 3.minutes))
    }

    @Test
    fun `isBeforeNow test`() {
        val timekeeper = Timekeeper(MutableTimeSource())

        val now = timekeeper.now()
        val behind = (now.milliseconds - 10.seconds).inWholeMilliseconds
        val after = (now.milliseconds + 10.seconds).inWholeMilliseconds
        assertTrue(timekeeper.isBeforeNow(behind))
        assertFalse(timekeeper.isBeforeNow(after))
    }

    @Test
    fun `isAfterNow test`() {
        val timekeeper = Timekeeper(MutableTimeSource())

        val now = timekeeper.now()
        val behind = (now.milliseconds - 10.seconds).inWholeMilliseconds
        val after = (now.milliseconds + 10.seconds).inWholeMilliseconds
        assertFalse(timekeeper.isAfterNow(behind))
        assertTrue(timekeeper.isAfterNow(after))
    }
}
