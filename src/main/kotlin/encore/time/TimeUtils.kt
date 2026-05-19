package encore.time

import io.ktor.util.date.*
import kotlin.time.Duration.Companion.minutes

/**
 * Returns `true` if the distance between [timestampMillis]
 * and current time is more than [minutes].
 */
fun isMoreThanMinutes(timestampMillis: Long, minutes: Int): Boolean {
    return getTimeMillis() - timestampMillis > minutes.minutes.inWholeMilliseconds
}
