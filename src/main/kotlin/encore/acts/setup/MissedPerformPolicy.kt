package encore.acts.setup

import encore.acts.StageAct

/**
 * Defines how a [StageAct] handles missed executions while inactive.
 *
 * Stage acts execute continuously while active. For persistable acts, any executions
 * missed during inactivity are resolved according to this policy upon resumption.
 *
 * @property maxBatch Maximum number of missed executions to process in a single [StageAct.perform] call.
 */
sealed class MissedPerformPolicy(val maxBatch: Int) {
    /**
     * Discards all missed executions (`maxBatch = 0`).
     */
    data object Skip : MissedPerformPolicy(maxBatch = 0)

    /**
     * Executes only the most recent missed execution (`maxBatch = 1`).
     */
    data object LastOnly : MissedPerformPolicy(maxBatch = 1)

    /**
     * Executes every missed executions up to a defined limit.
     *
     * @property maxTimes Maximum number of executions to process when catching up.
     *                   This should be bounded to prevent excessive workload unless
     *                   the [StageAct] implementation already handles it safely.
     */
    data class CatchUp(private val maxTimes: Int) : MissedPerformPolicy(maxBatch = maxTimes)
}
