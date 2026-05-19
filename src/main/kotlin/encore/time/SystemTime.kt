package encore.time

import io.ktor.util.date.getTimeMillis

/**
 * Default implementation of [TimeProvider] with the real system time.
 */
object SystemTime : TimeProvider {
    override fun now() = getTimeMillis()
}
