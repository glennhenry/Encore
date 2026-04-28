package encore.acts.photocard.model

import encore.tasks.ServerTaskDispatcher
import kotlinx.serialization.Serializable
import encore.acts.StageAct
import encore.acts.setup.ActSetup

/**
 * Represent the progress state of a [StageAct].
 *
 * @property startedAt Epoch millis of when the act was scheduled to run
 *                     (i.e., via [ServerTaskDispatcher.runTask]).
 * @property accumulatedDelay The accumulation of delay taken whilst running the act,
 *                            which includes the [ActSetup.initialDelay] and each repetition's
 *                            interval if the act is repeatable. This does not include the
 *                            act's execution time.
 * @property lastActiveAt Epoch millis of when the act was last active. This becomes the point where
 *                        `accumulatedDelay` can no longer be calculated manually.
 * @property performCount The total amount of times [StageAct.perform] has been called.
 */
@Serializable
data class ActProgress(
    var startedAt: Long,
    var accumulatedDelay: Long,
    var lastActiveAt: Long,
    var performCount: Int
)
