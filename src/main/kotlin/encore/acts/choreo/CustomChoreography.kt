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
interface CustomChoreography<T : ActConcept> : Choreography<T> {
    /**
     * Computes the next perform time.
     *
     * The returned value must be an absolute timestamp (epoch milliseconds),
     * not a delay. This ensures correct behavior across restarts and time shifts.
     *
     * @param concept The act input used to derive scheduling decisions.
     * @param context Execution and timing information.
     *
     * @return The next perform timestamp, or `null` if no further performs should occur.
     */
    fun next(concept: T, context: ChoreographyContext): Long?
}

/**
 * Execution and time context for [CustomChoreography].
 *
 * @property currentMillis The current time in epoch milliseconds.
 * @property performCount Number of times the act has already performed.
 * @property previousPerformAt Epoch millis of when previous perform was called.
 *                             `null` if this is the first.
 * @property startedAt Epoch millis of when the act was called to run.
 */
data class ChoreographyContext(
    val currentMillis: Long,
    val performCount: Int,
    val previousPerformAt: Long?,
    val startedAt: Long
)
