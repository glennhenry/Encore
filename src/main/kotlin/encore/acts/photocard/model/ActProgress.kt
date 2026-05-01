package encore.acts.photocard.model

import encore.acts.StageAct
import kotlinx.serialization.Serializable

/**
 * Represent the progress of a [StageAct].
 *
 * @property firstPerformAt Epoch millis of when this act is supposed to perform
 *                          for the first time.
 * @property performCount The total amount of times [StageAct.perform] has been called.
 * @property stoppedAt Epoch millis of when the act was stopped. `null` if it was never stopped.
 */
@Serializable
data class ActProgress(
    var firstPerformAt: Long,
    var performCount: Int,
    var stoppedAt: Long?
)
