package encore.acts.setup

import encore.acts.StageAct

/**
 * Describes [StageAct] performs portion.
 *
 * - [Once]
 * - [Repeat]
 * - [Forever]
 */
sealed class PerformMode {
    /**
     * The stage act is performed exactly once.
     */
    data object Once : PerformMode()

    /**
     * The stage act is performed repeatedly with a fixed interval.
     *
     * The total number of executions is `times + 1`, as [times] counts only
     * the additional repeats after the initial execution.
     *
     * Examples:
     * - `times = 0` is equivalent to [Once]
     * - `times = 1` is executed twice in total
     *
     * @property times Number of additional executions after the first run.
     * @property interval Interval between executions, in milliseconds.
     */
    data class Repeat(val times: Int, val interval: Long) : PerformMode()

    /**
     * The stage act is performed indefinitely at a fixed interval.
     *
     * @property interval Interval between executions, in milliseconds.
     */
    data class Forever(val interval: Long) : PerformMode()
}
