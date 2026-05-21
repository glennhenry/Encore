package encore.time

import encore.time.source.*
import kotlin.math.max
import kotlin.time.Duration
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
 * - Provide time utilies that relies on a time source (e.g., [isBeforeNow], [isAfterNow]).
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
     * Returns `true` if more than [minutes] of minutes have elapsed
     * since [timestampMillis].
     */
    fun hasElapsedBy(timestampMillis: Long, duration: Duration): Boolean {
        return now() > timestampMillis + duration.inWholeMilliseconds
    }

    /**
     * Returns the elapsed time since [timestampMillis].
     *
     * If [timestampMillis] is in the future, `0` is returned.
     */
    fun elapsedTimeSince(timestampMillis: Long): Long {
        return max(0, now() - timestampMillis)
    }

    /**
     * Returns the remaining time until [timestampMillis].
     *
     * If [timestampMillis] has already passed, `0` is returned.
     */
    fun remainingTime(timestampMillis: Long): Long {
        return max(0, timestampMillis - now())
    }

    /**
     * Returns whether [targetTime] is strictly before now.
     */
    fun isBeforeNow(targetTime: Long): Boolean {
        return now() > targetTime
    }

    /**
     * Returns whether [targetTime] is strictly after now.
     */
    fun isAfterNow(targetTime: Long): Boolean {
        return targetTime > now()
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
