package encore.time

import kotlin.time.Duration

/**
 * A test-focused implementation of [TimeProvider] which enables
 * the manual control of time, allowing to regulate time-constrained system components.
 */
class ManualTimeProvider(var currentTime: Long) : TimeProvider {
    override fun now(): Long = currentTime
    fun advance(ms: Long) { currentTime += ms }
    fun advance(duration: Duration) { currentTime += duration.inWholeMilliseconds }
}
