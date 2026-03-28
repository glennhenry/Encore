package testHelper

import encore.utils.TimeProvider
import kotlin.time.Duration

/**
 * A test-focused implementation of [TimeProvider] which enables
 * the control of time, allowing control to time-constrained system components.
 */
class FakeTimeProvider(var currentTime: Long) : TimeProvider {
    override fun now(): Long = currentTime
    fun advance(ms: Long) { currentTime += ms }
    fun advance(duration: Duration) { currentTime += duration.inWholeMilliseconds }
}
