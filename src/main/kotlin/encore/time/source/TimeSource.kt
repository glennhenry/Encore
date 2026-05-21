package encore.time.source

import encore.time.Timekeeper

/**
 * Represent an abstraction to a source of time.
 *
 * `TimeSource` is a low-level component that figures the way the system gets reference to time.
 * It decouples a system that depends on time with the underlying source.
 * This unlock an ability such as to provide a fake time which is controlled manually.
 *
 * Time source is typically not used directly by applications, instead a [Timekeeper]
 * is provided along with a specified `TimeSource` implementation.
 *
 * Implementations:
 * - [SystemTimeSource] based on real system time.
 * - [MutableTimeSource] allows alteration of time.
 * - [ScaledTimeSource] returns a scaled version of the time.
 * - [PausableTimeSource] able to pause/resume time model.
 */
interface TimeSource {
    /**
     * [TimeController] implementation.
     * This is typically implemented by the same [TimeSource] implementation itself.
     */
    val controller: TimeController

    /**
     * Return the current time in millis.
     */
    fun nowMillis(): Long
}
