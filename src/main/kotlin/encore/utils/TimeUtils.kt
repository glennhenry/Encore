package encore.utils

import java.time.Instant
import java.time.ZoneId

/**
 * Represents a specific time of day using a 24-hour clock.
 *
 * Usage:
 * ```
 * TimeOfDay(23, 15)
 * 23.at(15)
 * ```
 *
 * @property hour The hour of the day (0-23).
 * @property minute The minute of the hour (0-59).
 * @throws IllegalArgumentException If [hour] or [minute] are outside their valid ranges.
 */
data class TimeOfDay(val hour: Int, val minute: Int) {
    init {
        require(hour in 0..23) { "Hour must be between 0 and 23" }
        require(minute in 0..59) { "Minute must be between 0 and 59" }
    }

    /**
     * Return string representation that looks like `[23:15]`.
     */
    override fun toString(): String {
        return "[$hour:$minute]"
    }
}

/**
 * Fluent API to describe [TimeOfDay].
 */
fun Int.at(minute: Int): TimeOfDay {
    return TimeOfDay(this, minute)
}

/**
 * Computes the next wall-clock occurrence of this [TimeOfDay] relative to [now].
 *
 * For example, if `TimeOfDay` represents 15:00:
 * - If [now] is 14:00, this returns today at 15:00.
 * - If [now] is 16:00, this returns tomorrow at 15:00.
 *
 * The returned value is an absolute timestamp (epoch millis), not a delay.
 *
 * @param now the reference point in time (typically current time)
 * @param timezone the time zone used to interpret this [TimeOfDay]
 * @return epoch milliseconds of the next occurrence strictly after [now]
 */
fun TimeOfDay.nextOccurrence(now: Long, timezone: ZoneId): Long {
    val currentTime = Instant.ofEpochMilli(now).atZone(timezone)

    val targetTime = currentTime
        .withHour(hour)
        .withMinute(minute)
        .withSecond(0)
        .withNano(0)

    val next = if (targetTime.isAfter(currentTime)) {
        targetTime
    } else {
        targetTime.plusDays(1)
    }

    return next.toInstant().toEpochMilli()
}

/**
 * Represent a day in a week: [Monday], [Tuesday], [Wednesday],
 * [Thursday], [Friday], [Saturday], [Sunday]
 */
enum class DayOfWeek {
    Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday;

    /**
     * Return string representation that looks like `[Tuesday]`.
     */
    override fun toString(): String {
        return "[${this.name}]"
    }
}

/**
 * Converts [DayOfWeek] to Java's [java.time.DayOfWeek].
 */
fun DayOfWeek.toJavaDayOfWeek(): java.time.DayOfWeek {
    return when (this) {
        DayOfWeek.Monday -> java.time.DayOfWeek.MONDAY
        DayOfWeek.Tuesday -> java.time.DayOfWeek.TUESDAY
        DayOfWeek.Wednesday -> java.time.DayOfWeek.WEDNESDAY
        DayOfWeek.Thursday -> java.time.DayOfWeek.THURSDAY
        DayOfWeek.Friday -> java.time.DayOfWeek.FRIDAY
        DayOfWeek.Saturday -> java.time.DayOfWeek.SATURDAY
        DayOfWeek.Sunday -> java.time.DayOfWeek.SUNDAY
    }
}

/**
 * Computes the next wall-clock occurrence of this [DayOfWeek] with
 * [TimeOfDay] relative to [now].
 *
 * For example, if `DayOfWeek` and `TimeOfDay` represents Wednesday 15:00:
 * - If [now] is Tuesday 14:00, this returns tomorrow at 15:00.
 * - If [now] is Wednesday 16:00, this returns next week at 15:00.
 *
 * The returned value is an absolute timestamp (epoch millis), not a delay.
 *
 * @param now the reference point in time (typically current time)
 * @param timezone the time zone used to interpret this [TimeOfDay]
 * @return epoch milliseconds of the next occurrence strictly after [now]
 */
fun DayOfWeek.nextOccurrence(timeOfDay: TimeOfDay, now: Long, timezone: ZoneId): Long {
    val currentTime = Instant.ofEpochMilli(now).atZone(timezone)

    val targetDay = this.toJavaDayOfWeek()
    val currentDay = currentTime.dayOfWeek

    val daysToAdd = (targetDay.value - currentDay.value + 7) % 7

    var targetTime = currentTime
        .plusDays(daysToAdd.toLong())
        .withHour(timeOfDay.hour)
        .withMinute(timeOfDay.minute)
        .withSecond(0)
        .withNano(0)

    if (!targetTime.isAfter(currentTime)) {
        targetTime = targetTime.plusWeeks(1)
    }

    return targetTime.toInstant().toEpochMilli()
}
