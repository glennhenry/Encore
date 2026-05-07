package encore.acts.choreo

import encore.acts.ActConcept
import encore.acts.StageAct

/**
 * Advanced scheduling model for a [StageAct].
 *
 * Implementations define their own time model by computing the next perform
 * timestamp dynamically.
 *
 * This is useful for:
 * - context-dependent schedules (e.g. player state)
 * - dynamic intervals
 * - non-uniform or staged perform patterns
 *
 * @param T The [ActConcept] associated with the [StageAct].
 */
interface CustomChoreography<T : ActConcept> : Choreography {
    /**
     * Computes the next perform time.
     *
     * The returned value must be an absolute timestamp (epoch milliseconds),
     * not a delay. This ensures correct behavior across restarts and time shifts.
     *
     * @param currentPerformCount Number of times the act has already performed.
     * @param concept The act input used to derive scheduling decisions.
     * @param currentMillis The current time in epoch milliseconds.
     *
     * @return The next perform timestamp, or `null` if no further performs should occur.
     */
    fun next(currentPerformCount: Int, concept: T, currentMillis: Long): Long?
}
