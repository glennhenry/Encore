package encore.acts.choreo

import SystemTimezone
import encore.acts.ActConcept
import encore.acts.StageAct
import encore.utils.TimeOfDay
import encore.utils.nextOccurrence

/**
 * Schedules a [StageAct] to perform once per day at a fixed [TimeOfDay].
 *
 * Perform is aligned to wall-clock time and repeats indefinitely
 * (unless explicitly stopped).
 *
 * @param T The [ActConcept] associated with the [StageAct].
 * @property runAt The time of day at which the act should perform.
 */
data class DailyChoreography<T : ActConcept>(
    val runAt: TimeOfDay
) : CustomChoreography<T> {
    override fun next(concept: T, context: ChoreographyContext): Long {
        return runAt.nextOccurrence(context.currentMillis, SystemTimezone)
    }
}
