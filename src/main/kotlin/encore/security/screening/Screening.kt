package encore.security.screening

import encore.fancam.Fancam

/**
 * DSL-style API to run a series of predicate upon an object.
 *
 * A screening process is composed of one or more validation stages.
 * Each validation step is expressed using [check], which takes a predicate
 * that **must evaluate to true**. Multiple rules can be chained before
 * actually ending the DSL and validating them all in [finalize].
 *
 * Example:
 * ```kotlin
 * Screening("BuildingCreate resource check") { playerData }
 *     .checkFor(playerId)
 *     .check("Resource must be more than 100") { getResources() > 100 }
 *     .check("Require at least 20XP") { getXP() >= 20 }
 *     .finalize(failStrategy = FailStrategy.Disconnect)
 * ```
 *
 * Evaluation details:
 * - Validation will stop at the first failed [check] stage.
 * - The fail strategy and reason are determined by:
 *   1. The stage's own `failStrategy` and `failReason` if defined.
 *   2. The global ones passed to [finalize].
 *   3. Defaults: `FailStrategy.Cancel` and reason `"[Not specified]"`.
 * - Use [checkSuspend] and [finalizeSuspend] if the following validation scheme
 *   contains suspended function calls. Can use `require` and `requireSuspend` together,
 *   but must use `validateSuspend` if there is at least one `requireSuspend`.
 *
 * @param schemeName A readable alias or identifier for this validation scheme (e.g., `"BuildingCreate"`).
 * @param factory A factory lambda that provides the execution context (e.g., `PlayerServices`).
 * @param T The type of the validation context provided by [factory].
 */
class Screening<T>(private val schemeName: String, private val factory: () -> T) {
    private var target: String = "<Undefined>"
    private val stages = mutableListOf<ScreeningStage<T>>()

    /**
     * Defines the logical target of this validation (e.g., a player ID, username).
     *
     * Used primarily for logging and debugging. If not specified,
     * defaults to `<Undefined>`.
     */
    fun checkFor(target: String) = apply { this.target = target }

    /**
     * Adds a validation stage to the scheme.
     *
     * Each stage defines a named predicate that **must evaluate to `true`**.
     * If it fails, the associated [FailStrategy] and reason will be used
     * when reporting or handling the failure.
     *
     * @param stageName Short descriptive title for this stage (e.g., `"XP Check"`).
     * @param failStrategy Optional strategy that determines how failure is handled.
     * @param failReason Optional reason string describing why the validation is required.
     * @param predicate The actual validation check to run. **Must be true for the check to pass.**
     */
    fun check(
        stageName: String,
        failStrategy: FailStrategy = FailStrategy.Cancel,
        failReason: String = "[Not specified]",
        predicate: T.() -> Boolean
    ) = apply {
        stages.add(
            ScreeningStage(stageName, failStrategy, failReason, NonSuspendPredicate(predicate))
        )
    }

    /**
     * Suspended version of [check].
     *
     * This takes suspendable predicate function.
     */
    fun checkSuspend(
        stageName: String,
        failStrategy: FailStrategy = FailStrategy.Cancel,
        failReason: String = "[Not specified]",
        predicate: suspend T.() -> Boolean
    ) = apply {
        stages.add(
            ScreeningStage(stageName, failStrategy, failReason, SuspendPredicate(predicate))
        )
    }

    /**
     * Executes all registered validation stages sequentially.
     *
     * Stops immediately when the first validation fails.
     *
     * The [ScreeningResult] will be one of:
     * - [ScreeningResult.Passed] = All checks succeeded.
     * - [ScreeningResult.Failed] = Failed at a particular stage.
     * - [ScreeningResult.Error] = Exception occurred during validation.
     *
     * The effective [FailStrategy] and reason follow this priority:
     * 1. Stage-level values (if set in [check]).
     * 2. Values passed to this method.
     * 3. Default fallbacks (`Cancel`, "[Not specified]").
     *
     * @param failStrategy Default failure handling strategy.
     * @param failReason Default reason for failure if not overridden.
     * @return A [ScreeningResult] describing the outcome.
     */
    fun finalize(
        failStrategy: FailStrategy = FailStrategy.Cancel,
        failReason: String = "[Not specified]"
    ): ScreeningResult {
        val instance = factory()

        for ((index, stage) in stages.withIndex()) {
            val name = if (stage.name.isEmpty()) "stage-$index" else "stage-$index: ${stage.name}"
            val strategy = stage.failStrategy ?: failStrategy
            val reason = stage.failReason ?: failReason

            val passed = try {
                stage.predicate.check(instance)
            } catch (e: Exception) {
                Fancam.error { "Error during validation check of '$schemeName' ($name) for target=$target: ${e.message}" }
                return ScreeningResult.Error(strategy, reason, name, e)
            }

            if (!passed) {
                return ScreeningResult.Failed(strategy, reason, name)
            }
        }

        return ScreeningResult.Passed
    }

    /**
     * Suspended version of [check].
     *
     * This executes the predicate function in suspendable context.
     */
    suspend fun finalizeSuspend(
        failStrategy: FailStrategy = FailStrategy.Cancel,
        failReason: String = "[Not specified]"
    ): ScreeningResult {
        val instance = factory()

        for ((index, stage) in stages.withIndex()) {
            val name = if (stage.name.isEmpty()) "stage-$index" else "stage-$index: ${stage.name}"
            val strategy = stage.failStrategy ?: failStrategy
            val reason = stage.failReason ?: failReason

            val passed = try {
                stage.predicate.checkSuspend(instance)
            } catch (e: Exception) {
                Fancam.error { "Error during validation check of '$schemeName' ($name) for target=$target" }
                return ScreeningResult.Error(strategy, reason, name, e)
            }

            if (!passed) {
                return ScreeningResult.Failed(strategy, reason, name)
            }
        }

        return ScreeningResult.Passed
    }
}
