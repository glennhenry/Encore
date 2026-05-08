package encore.acts.choreo

import encore.acts.ActConcept
import encore.acts.StageAct
import kotlin.time.Duration

/**
 * Basic scheduling configuration for a [StageAct].
 *
 * This model is suitable for simple tasks that:
 * - start after a fixed delay, and
 * - optionally repeat with a fixed interval.
 *
 * @property initialDelay The delay before the first performs.
 * @property performMode Defines the act performs portion.
 */
data class BasicChoreography<T : ActConcept>(
    val initialDelay: Duration, val performMode: PerformMode
) : Choreography<T>
