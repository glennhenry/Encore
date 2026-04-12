package encore.utils

/**
 * Represents the summary of an operation, capturing success with a value
 * or failure without exposing exceptions.
 *
 * Use [Outcome.Ok] when the operation succeeded and [Outcome.Fail] when it fails.
 *
 * This type shares the similar idea with [Result], but intentionally
 * does not carry a `Throwable`. It is designed for cases where:
 * - The caller requests an operation from another component.
 * - The caller only cares about whether the operation succeeded or failed
 *   and the value it produces from the success case.
 * - The caller does not need the underlying exception or error details.
 *
 * It is also similar to [Report] but outcome has value on the success case.
 *
 * @param T The type of value produced when the operation succeeds.
 */
sealed interface Outcome<out T> {
    /**
     * Indicates that the operation succeeded.
     *
     * @property value The result of the successful operation.
     */
    data class Ok<T>(val value: T) : Outcome<T>

    /**
     * Indicates that the operation failed.
     */
    data object Fail : Outcome<Nothing>
}

/**
 * Returns whether this outcome is [Ok].
 */
fun <T> Outcome<T>.isOk(): Boolean = this is Outcome.Ok

/**
 * Returns whether this outcome is [Fail].
 */
fun <T> Outcome<T>.isFail(): Boolean = this is Outcome.Fail

/**
 * Returns the value of this outcome or `null` if it fails.
 */
fun <T> Outcome<T>.okOrNull(): T? = (this as? Outcome.Ok)?.value

/**
 * Returns the value if this is [Outcome.Ok], otherwise throws an error.
 *
 * This is intended for cases where failure is unexpected and should fail-fast.
 *
 * @param failMessage Additional context for the failure.
 * @throws IllegalStateException if this is [Outcome.Fail]
 */
fun <T> Outcome<T>.require(failMessage: () -> String): T {
    return when (this) {
        is Outcome.Ok -> value
        is Outcome.Fail -> {
            error("Expected Outcome.Ok but was Fail: ${failMessage()}")
        }
    }
}

/**
 * Executes one of the given functions depending on the type of this [Outcome]
 * and returns its result.
 *
 * Example usage:
 * ```
 * val outcome: Outcome<Int> = subunit.collectResources()
 * val message: Response = outcome.fold(
 *     onOk = { value -> SuccessResponse("Resource collected: $value") },
 *     onFail = { FailedResponse("Failed to collect resource") }
 * )
 * ```
 *
 * @param onOk Lambda to execute when the outcome is [Outcome.Ok].
 * @param onFail Lambda to execute when the outcome is [Outcome.Fail].
 * @return The execution result of either `onOk` or `onFail`.
 */
inline fun <T, R> Outcome<T>.fold(onOk: (T) -> R, onFail: () -> R): R {
    return when (this) {
        is Outcome.Ok -> onOk(value)
        Outcome.Fail -> onFail()
    }
}

/**
 * Executes one of the given functions depending on the type of this [Outcome].
 *
 * This does not return any value.
 *
 * Example usage:
 * ```
 * val outcome: Outcome<Int> = character.getDamage()
 * outcome.handles(
 *     onOk = { damage -> enemy.decreaseBy(damage) },
 *     onFail = { Fancam.error { "Error when getting damage on attack request" } }
 * )
 * ```
 *
 * @param onOk Lambda to execute when the outcome is [Outcome.Ok].
 * @param onFail Lambda to execute when the outcome is [Outcome.Fail].
 */
inline fun <T> Outcome<T>.handles(onOk: (T) -> Unit, onFail: () -> Unit) {
    return when (this) {
        is Outcome.Ok -> onOk(value)
        Outcome.Fail -> onFail()
    }
}

/**
 * Converts a [Result] into an [Outcome] by:
 * - Invoking [onSuccess] if the result is successful, then returns [Outcome.Ok].
 * - Invoking [onFailure] if the result is a failure, then returns [Outcome.Fail].
 */
fun <T> Result<T>.toOutcome(
    onSuccess: (T) -> Unit = {},
    onFailure: (Throwable) -> Unit
): Outcome<T> {
    return fold(
        onSuccess = {
            onSuccess(it)
            Outcome.Ok(it)
        },
        onFailure = {
            onFailure(it)
            Outcome.Fail
        }
    )
}
