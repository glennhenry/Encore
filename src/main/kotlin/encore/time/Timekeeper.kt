package encore.time

import encore.time.source.MutableTimeSource
import encore.time.source.PausableTimeSource
import encore.time.source.ScaledTimeSource
import encore.time.source.SystemTimeSource
import encore.time.source.TimeController
import encore.time.source.TimeSource
import kotlin.time.Duration.Companion.minutes

/**
 * Provides time-related APIs.
 *
 * `Timekeeper` acts as the main entry point for consulting time and performing
 * time-based operations.
 *
 * Usage:
 * - Using the given [TimeSource], retrieve the current system time with [now].
 * - Manipulates time from [TimeController] via [control].
 * - Provide various time utilies which relies on the same time source
 *   to get the time reference.
 *
 * Application components that relies on time should depend on [Timekeeper] instead
 * (e.g., replaces [System.currentTimeMillis]).
 * `Timekeeper` is typically accessed from [TimeCenter].
 *
 * @property source The underlying source responsible for producing time values
 */
class Timekeeper(private val source: TimeSource) {
    /**
     * Access point to the this timekeeper's manipulation behavior.
     */
    val control: TimeController = source.controller

    /**
     * Returns the current time in epoch milliseconds for this timekeeper.
     */
    fun now(): Long {
        return source.nowMillis()
    }

    /**
     * Returns `true` if the distance between [timestampMillis]
     * and current time is more than [minutes].
     */
    fun isMoreThanMinutes(timestampMillis: Long, minutes: Int): Boolean {
        return now() - timestampMillis > minutes.minutes.inWholeMilliseconds
    }
}

/**
 * Shorthand to create a [Timekeeper] with [SystemTimeSource].
 */
val SystemTimekeeper = Timekeeper(SystemTimeSource())

/**
 * Shorthand to create a [Timekeeper] with [MutableTimeSource].
 */
val MutableTimekeeper = Timekeeper(MutableTimeSource())

/**
 * Shorthand to create a [Timekeeper] with [ScaledTimeSource].
 */
val ScaledTimekeeper = Timekeeper(ScaledTimeSource())

/**
 * Shorthand to create a [Timekeeper] with [PausableTimeSource].
 */
val PausableTimekeeper = Timekeeper(PausableTimeSource())
