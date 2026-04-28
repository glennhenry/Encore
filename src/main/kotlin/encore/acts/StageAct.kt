package encore.acts

import encore.acts.setup.ActSetup
import encore.tasks.CancellationReason
import encore.tasks.ServerTaskDispatcher

/**
 * Represents a scheduled server-side task.
 *
 * A `StageAct` is a deferred execution bound to a runtime owner (e.g., a player or the server)
 * with a defined lifecycle. It takes an [ActConcept] as input for execution.
 *
 * Stage acts are typically used for time-based or repeated operations. For example,
 * a building construction that completes after 5 minutes can be modeled as a `StageAct`
 * with a 5 minutes delay, and will notify player of its completion inside [perform].
 *
 * #### Identification
 *
 * Each stage act must define a unique identifier via [createId]. This ID must be globally
 * unique across all stage act instances. It is typically derived from domain-specific
 * identifiers (e.g., `playerId`, `buildingId`).
 *
 * [createIdentity] provides additional key-value data to distinguish multiple instances
 * of the same `StageAct` type. For example, multiple construction tasks may share the
 * same type but differ by `buildingId` and `buildDuration`.
 *
 * #### Configuration
 *
 * The execution behavior is defined by [createSetup], producing an [ActSetup] including
 * `initialDelay`, [PerformMode], and [LifetimeMode].
 *
 * #### Lifecycle
 *
 * The stage act system offers 4 lifecycles:
 * - [onStart]: Invoked once when the act is first scheduled to run
 *              (e.g., by calling [ServerTaskDispatcher.runTask]).
 * - [perform]: The main execution step. Invoked once or repeatedly depending on [PerformMode].
 * - [onEndingFairy]: Invoked once when the act completes all scheduled executions.
 *     - [PerformMode.Once]: called immediately after [perform]
 *     - [PerformMode.Repeat]: called after the final [perform]
 *     - [PerformMode.Forever]: never called
 * - [onCancelled]: Invoked if the act is cancelled before normal completion
(e.g., via [ServerTaskDispatcher.stopTask]).
 *
 * @param T The type of [ActConcept] for this stage act.
 */
interface StageAct<T : ActConcept> {
    /**
     * The name of this `StageAct`, used for debugging and identification on [Photocard.name].
     */
    val name: String

    /**
     * Produces a globally unique identifier for this act instance.
     */
    fun createId(concept: T): String

    /**
     * Produces additional identity data for distinguishing instances of the same act type.
     */
    fun createIdentity(concept: T): Map<String, String> = emptyMap()

    /**
     * Produces the execution configuration for this act.
     */
    fun createSetup(concept: T): ActSetup

    /**
     * Called once when the act is first scheduled.
     * Use for initialization or side effects (e.g., notifying a client).
     */
    suspend fun onStart(concept: T) = Unit

    /**
     * Main execution body of the act.
     *
     * [times] represents the batch size for the current [perform] call.
     *
     * For example, if 6 executions were missed and a [MissedPerformPolicy.CatchUp] is applied,
     * `times` will be 6. Under normal conditions (no missed executions), `times` is typically 1.
     *
     * Stage act should implement `perform` in inclusion of the arbitrary amount of [times],
     * unless [MissedPerformPolicy] is specifically skip or last only.
     *
     * @param times Number of executions to process in a single batch.
     */
    suspend fun perform(concept: T, times: Int)

    /**
     * Called once when the act completes all scheduled executions successfully.
     */
    suspend fun onEndingFairy(concept: T) = Unit

    /**
     * Called if the act is cancelled before completing normally.
     *
     * @param reason The cause of cancellation.
     */
    suspend fun onCancelled(concept: T, reason: CancellationReason) = Unit
}
