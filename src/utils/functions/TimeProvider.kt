package utils.functions

import io.ktor.util.date.getTimeMillis

/**
 * Represent unit capable of supplying time.
 *
 * Instead of using `System.currentTimeMillis()`, server components that may have
 * the potential to be unit tested are encouraged to depend on this instead.
 * This is to ensure components are testable.
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
