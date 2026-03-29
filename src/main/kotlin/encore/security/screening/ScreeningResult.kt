package encore.security.screening

/**
 * Result of executing a [Screening].
 *
 * A screening can:
 * - complete successfully ([Passed])
 * - stop at a failing stage ([Failed])
 * - abort due to an exception ([Error])
 */
sealed class ScreeningResult {
    /**
     * All stages passed successfully.
     */
    object Passed : ScreeningResult()

    /**
     * Execution stopped because a stage returned `false`.
     *
     * @param stageIndex Index of the first failing stage (counting starts from 1).
     */
    data class Failed(val stageIndex: Int) : ScreeningResult()

    /**
     * Execution aborted due to an exception thrown during a stage.
     *
     * @param stageIndex Index of the stage where the error occurred (counting starts from 1).
     * @param error The thrown exception.
     */
    data class Error(val stageIndex: Int, val error: Throwable) : ScreeningResult()
}
