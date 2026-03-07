import utils.functions.TimeProvider
import kotlin.time.Duration

/**
 * Fake time provider to easily advance time.
 */
class FakeTimeProvider(var currentTime: Long) : TimeProvider {
    override fun now(): Long = currentTime
    fun advance(ms: Long) { currentTime += ms }
    fun advance(duration: Duration) { currentTime += duration.inWholeMilliseconds }
}
