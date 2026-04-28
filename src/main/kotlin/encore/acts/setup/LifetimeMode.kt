package encore.acts.setup

import encore.acts.StageAct

/**
 * Defines the lifetime behavior of a [StageAct].
 *
 * - [Bound] represents temporary acts tied to some runtime owner
 *   (e.g., a player's connection). The act is terminated when its owner is no longer valid,
 *   even if it has not completed.
 * - [PausedPersistent] and [ContinuousPersistent]
 *   represents durable acts that are stored and can be resumed later.
 */
sealed class LifetimeMode {
    /**
     * A temporary act bound to a runtime owner.
     */
    data object Bound : LifetimeMode()

    /**
     * A persistent act that survives runtime bounds and resumes from its
     * last execution state.
     *
     * Time does not progress while inactive. Upon resumption, the act continues
     * as if no time has passed.
     */
    data object PausedPersistent : LifetimeMode()

    /**
     * A persistent act that survives runtime bounds and preserves continuous
     * time progression.
     *
     * While inactive, time continues to advance. Upon resumption, the act
     * may execute missed intervals and resume mid-interval based on real time.
     *
     * @property missedPerformPolicy Execution policy applied when the act is resumed
     *                               if one or more executions are missed.
     */
    data class ContinuousPersistent(val missedPerformPolicy: MissedPerformPolicy) : LifetimeMode()
}
