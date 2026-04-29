package testHelper

import encore.utils.TimeProvider
import encore.utils.FakeTimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope

/**
 * A test-focused implementation of [TimeProvider].
 *
 * Unlike [FakeTimeProvider], the time used here is not user-supplied, but rather
 * based on the virtual time of [TestScope]. The control of time is also automatic.
 */
class TestTimeProvider(
    private val scope: TestScope
) : TimeProvider {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun now(): Long = scope.testScheduler.currentTime
}
