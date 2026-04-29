package encore.utils

import io.ktor.util.date.getTimeMillis
import kotlin.time.Duration

/**
 * Represent a component capable of supplying time.
 *
 * Server components that relies on time are encouraged to use this interface.
 * This allows the ability to control time in unit tests.
 *
 * Use [SystemTime] for non-tests system.
 */
fun interface TimeProvider {
    fun now(): Long
}

/**
 * Default implementation of [TimeProvider] with the real system time.
 */
object SystemTime : TimeProvider {
    override fun now() = getTimeMillis()
}

/**
 * A test-focused implementation of [TimeProvider] which enables
 * the manual control of time, allowing to regulate time-constrained system components.
 */
class ManualTimeProvider(var currentTime: Long) : TimeProvider {
    override fun now(): Long = currentTime
    fun advance(ms: Long) { currentTime += ms }
    fun advance(duration: Duration) { currentTime += duration.inWholeMilliseconds }
}
