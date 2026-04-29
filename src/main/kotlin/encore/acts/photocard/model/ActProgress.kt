package encore.acts.photocard.model

import encore.tasks.ServerTaskDispatcher
import kotlinx.serialization.Serializable
import encore.acts.StageAct

/**
 * Represent the progress of an unfinished [StageAct].
 *
 * @property startedAt Epoch millis of when the act was scheduled to run
 *                     (i.e., via [ServerTaskDispatcher.runTask]).
 * @property remainingDelay Represent the delay left before the next perform.
 * @property lastActiveAt Epoch millis of when the act was last active.
 *                        This value reflects the instant of when act's cancellation occured.
 * @property performCount The total amount of times [StageAct.perform] has been called.
 */
@Serializable
data class ActProgress(
    var startedAt: Long,
    var remainingDelay: Long,
    var lastActiveAt: Long,
    var performCount: Int
)
