package testUtils

import encore.time.source.TimeController
import encore.time.source.TimeSource
import encore.time.Timekeeper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope

/**
 * A test-focused implementation of [TimeSource].
 *
 * The time here is based on the virtual time supplied by [TestScope].
 */
class VirtualTimeSource(private val scope: TestScope) : TimeSource, TimeController {
    override val controller: TimeController = this

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun nowMillis(): Long = scope.testScheduler.currentTime
}

/**
 * Shorthand to create a [Timekeeper] with [VirtualTimeSource].
 */
fun virtualTimekeeper(scope: TestScope): Timekeeper {
    return Timekeeper(VirtualTimeSource(scope))
}
