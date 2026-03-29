package encore.security.screening

import encore.fancam.Fancam

/**
 * A DSL-style validator that executes a sequence of predicate checks on a target object.
 *
 * A [Screening] consists of ordered validation stages added via [check] or [checkSuspend].
 * Each stage must return `true` to pass. Evaluation stops at the first failure or error.
 *
 * Example:
 * ```kotlin
 * Screening("BuildingCreate resource check") { playerData }
 *     .checkFor(playerId)
 *     .check("Resource must be more than 100") { getResources() > 100 }
 *     .check("Require at least 20 XP") { getXP() >= 20 }
 *     .finish()
 * ```
 *
 * Execution rules:
 * - Stages are executed sequentially in the order they are added.
 * - Evaluation stops at the first failed stage, returning [ScreeningResult.Failed]
 * - If a stage throws, execution stops and returns [ScreeningResult.Error].
 * - If any suspendable stage is added, [finishSuspend] must be used.
 *
 * @param title Descriptive name of this screening (e.g., "BuildingCreate").
 * @param factory Provides the validation context for each stage.
 */
class Screening<T>(private val title: String, private val factory: () -> T) {
    private var target: String = "<Undefined>"
    private val stages = mutableListOf<ScreeningStage<T>>()

    /**
     * Defines the logical target of this screening (e.g., player ID, username).
     *
     * This is used for logging.
     */
    fun checkFor(target: String) = apply {
        this.target = target
    }

    /**
     * Adds a synchronous validation stage.
     *
     * @param description Descriptive text of the condition being checked.
     * @param predicate Must return `true` for the stage to pass.
     */
    fun check(
        description: String,
        predicate: T.() -> Boolean
    ) = apply {
        stages += ScreeningStage(description, NonSuspendPredicate(predicate))
    }

    /**
     * Adds a suspendable validation stage.
     */
    fun checkSuspend(
        description: String,
        predicate: suspend T.() -> Boolean
    ) = apply {
        stages += ScreeningStage(description, SuspendPredicate(predicate))
    }

    /**
     * Executes all registered validation stages sequentially,
     * stops immediately when the first validation fails.
     *
     * @return [ScreeningResult] describing the outcome.
     */
    fun finish(): ScreeningResult {
        Fancam.trace { "Screening '$title' started (target=$target)" }
        val instance = factory()

        for ((index, stage) in stages.withIndex()) {
            val stageIndex = index + 1
            val stageDesription = stage.label(stageIndex)

            val passed = try {
                stage.predicate.check(instance)
            } catch (e: Throwable) {
                Fancam.error {
                    "Screening '$title' error at $stageDesription (target=$target): ${e.message}"
                }
                return ScreeningResult.Error(stageIndex, e)
            }

            if (!passed) {
                Fancam.trace { "Screening '$title' failed at $stageDesription (target=$target)" }
                return ScreeningResult.Failed(stageIndex)
            }

            Fancam.trace { "Screening '$title' passed $stageDesription (target=$target)" }
        }

        return ScreeningResult.Passed
    }

    /**
     * Suspended version of [finish].
     *
     * This executes the predicate function in suspendable context.
     */
    suspend fun finishSuspend(): ScreeningResult {
        Fancam.trace { "Screening '$title' started (target=$target)" }
        val instance = factory()

        for ((index, stage) in stages.withIndex()) {
            val stageIndex = index + 1
            val stageDesription = stage.label(stageIndex)

            val passed = try {
                stage.predicate.checkSuspend(instance)
            } catch (e: Throwable) {
                Fancam.error {
                    "Screening '$title' error at $stageDesription (target=$target): ${e.message}"
                }
                return ScreeningResult.Error(stageIndex, e)
            }

            if (!passed) {
                Fancam.trace { "Screening '$title' failed at $stageDesription (target=$target)" }
                return ScreeningResult.Failed(stageIndex)
            }

            Fancam.trace { "Screening '$title' passed $stageDesription (target=$target)" }
        }

        return ScreeningResult.Passed
    }
}

/**
 * Formats stage label for logging/debugging.
 */
private fun <T> ScreeningStage<T>.label(index: Int): String =
    if (description.isBlank()) "stage-$index"
    else "stage-$index: $description"
