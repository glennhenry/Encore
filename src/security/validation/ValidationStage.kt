package security.validation

/**
 * Represents a single validation stage within a [ValidationScheme].
 *
 * Each stage performs a single logical check (predicate) and may optionally define
 * its own failure handling behavior via [failStrategy] and [failReason].
 *
 * @param name Human-readable label for the stage.
 * @param failStrategy Optional override for how this stage handles failure.
 * @param failReason Optional description of why this validation is required.
 * @param predicate The condition to evaluate; should return `true` if valid.
 *                  Can be made suspendable via `Predicate` interface.
 */
data class ValidationStage<T>(
    val name: String = "",
    val failStrategy: FailStrategy? = null,
    val failReason: String? = null,
    val predicate: Predicate<T>
)

/**
 * Wrapper of predicate to enforce either [check] or [checkSuspend].
 */
sealed interface Predicate<T> {
    fun check(ctx: T): Boolean
    suspend fun checkSuspend(ctx: T): Boolean
}

/**
 * Non-suspendable predicate evaluation.
 */
class NonSuspendPredicate<T>(
    private val block: T.() -> Boolean
) : Predicate<T> {
    override fun check(ctx: T) = ctx.block()
    override suspend fun checkSuspend(ctx: T) = ctx.block()
}

/**
 * Suspendable predicate evaluation.
 *
 * Using [check] will throw `IllegalStateException` error.
 */
class SuspendPredicate<T>(
    private val block: suspend T.() -> Boolean
) : Predicate<T> {
    override fun check(ctx: T): Boolean =
        error("Suspend predicate used in non-suspend validation")

    override suspend fun checkSuspend(ctx: T) = ctx.block()
}
