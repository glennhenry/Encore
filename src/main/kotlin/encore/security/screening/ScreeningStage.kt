package encore.security.screening

/**
 * A single validation step within a [Screening].
 *
 * Each stage represents a condition that must evaluate to `true`
 * for the screening to continue. Stages are executed in order and
 * evaluation stops at the first failure.
 *
 * @param description Explanation of the condition being tested.
 *                    Also used for identification during debugging when
 *                    the stage fails or throws.
 * @param predicate Condition to evaluate. Should return `true` if the
 *                  stage passes. May be suspendable via [Predicate].
 */
data class ScreeningStage<T>(
    val description: String = "",
    val predicate: Predicate<T>
)

/**
 * Wrapper of predicate input, enforcing either [check] or [checkSuspend].
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
    override fun check(ctx: T): Boolean = error("Suspend predicate used in non-suspend validation")
    override suspend fun checkSuspend(ctx: T) = ctx.block()
}
