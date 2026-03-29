package encore.security.screening

/**
 * Represents the outcome of a [Screening] execution.
 *
 * A validation may either **pass**, **fail**, or **encounter an error**.
 * Each non-success result carries contextual information such as the
 * associated [FailStrategy], reason, and any captured exception.
 */
sealed class ScreeningResult(
    val failStrategy: FailStrategy? = null,
    val failReason: String? = null,
    val failedAtStage: String? = null,
    val error: Throwable? = null
) {
    /**
     * The validation passed successfully where all conditions were met.
     */
    object Passed : ScreeningResult()

    /**
     * The validation failed because one or more conditions did not meet the requirements.
     */
    class Failed(failStrategy: FailStrategy, failReason: String, failedAtStage: String) : ScreeningResult(failStrategy, failReason, failedAtStage)

    /**
     * The validation could not be performed due to an internal error.
     */
    class Error(failStrategy: FailStrategy, failReason: String, failedAtStage: String, error: Throwable) :
        ScreeningResult(failStrategy, failReason, failedAtStage, error)
}
