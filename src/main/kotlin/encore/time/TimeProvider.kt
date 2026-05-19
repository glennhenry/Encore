package encore.time

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
