package testUtils

import encore.time.TimeProvider
import encore.time.ManualTimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope

/**
 * A test-focused implementation of [TimeProvider].
 *
 * Unlike [ManualTimeProvider], the time used here is not user-supplied, but rather
 * based on the virtual time of [TestScope]. The control of time is also automatic.
 */
class VirtualTimeProvider(
    private val scope: TestScope
) : TimeProvider {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun now(): Long = scope.testScheduler.currentTime
}
