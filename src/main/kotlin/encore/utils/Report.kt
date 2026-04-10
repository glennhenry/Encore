package encore.utils

/**
 * Represents the summary of an operation, capturing success or failure
 * without exposing exceptions.
 *
 * This type shares the similar idea with [Result], but intentionally
 * does not carry a `Throwable`. It is designed for cases where:
 * - The caller requests an operation from another component.
 * - The caller only cares about whether the operation succeeded or failed.
 * - The caller does not need the underlying exception or error details.
 *
 * `Report` type supports safe handling of operations while avoiding
 * exception leakage or duplicate logging.
 *
 * @param T The type of value produced when the operation succeeds.
 */
sealed interface Report<out T> {
    /**
     * Indicates that the operation succeeded.
     *
     * @property value The result of the successful operation.
     */
    data class Ok<T>(val value: T) : Report<T>

    /**
     * Indicates that the operation failed.
     */
    data object Fail : Report<Nothing>
}

/**
 * Returns whether this report is [Ok].
 */
fun <T> Report<T>.isOk(): Boolean = this is Report.Ok

/**
 * Returns whether this report is [Fail].
 */
fun <T> Report<T>.isFail(): Boolean = this is Report.Fail

/**
 * Returns the value of this report or `null` if it fails.
 */
fun <T> Report<T>.okOrNull(): T? = (this as? Report.Ok)?.value

/**
 * Returns the value if this is [Report.Ok], otherwise throws an error.
 *
 * This is intended for cases where failure is unexpected and should fail-fast.
 *
 * @param failMessage Additional context for the failure.
 * @throws IllegalStateException if this is [Report.Fail]
 */
fun <T> Report<T>.require(failMessage: () -> String): T {
    return when (this) {
        is Report.Ok -> value
        is Report.Fail -> {
            error("Expected Report.Ok but was Fail: ${failMessage()}")
        }
    }
}

/**
 * Returns [Report.Ok] when this value is not null, otherwise [Report.Fail] will be returned.
 */
fun <T> T?.toReportOkOrFail(): Report<T> = this?.let { Report.Ok(it) } ?: Report.Fail

/**
 * Executes one of the provided lambdas depending on the outcome of this [Report].
 *
 * This is a convenient way to handle both success (`Ok`) and failure (`Fail`) cases
 * in a single expression, without needing explicit `when` statements.
 *
 * Example usage:
 * ```
 * val result: Report<Int> = service.getValue()
 * val message = result.handles(
 *     onOk = { value -> SuccessResponse("Success: $value") },
 *     onFail = { FailedResponse("Operation failed") }
 * )
 * ```
 *
 * @param onOk Lambda to execute when the report is [Report.Ok].
 *             Receives a value as the parameter.
 * @param onFail Lambda to execute when the report is [Report.Fail].
 * @return The result of either `onOk` or `onFail`.
 */
inline fun <T, R> Report<T>.handles(onOk: (T) -> R, onFail: () -> R): R {
    return when (this) {
        is Report.Ok -> onOk(value)
        Report.Fail -> onFail()
    }
}

/**
 * Converts a [Result] into a [Report].
 *
 * - Invokes [onSuccess] if the result is successful, then returns [Report.Ok].
 * - Invokes [onFailure] if the result is a failure, then returns [Report.Fail].
 */
fun <T> Result<T>.toReport(
    onSuccess: (T) -> Unit = {},
    onFailure: (Throwable) -> Unit
): Report<T> {
    return fold(
        onSuccess = {
            onSuccess(it)
            Report.Ok(it)
        },
        onFailure = {
            onFailure(it)
            Report.Fail
        }
    )
}
