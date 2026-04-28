package encore.acts.photocard.model

import kotlinx.serialization.Serializable
import encore.acts.StageAct
import encore.acts.setup.LifetimeMode

/**
 * Represents a snapshot of an unfinished [StageAct].
 *
 * Stage acts with [LifetimeMode.PausedPersistent] or [LifetimeMode.ContinuousPersistent]
 * that are not completed when a player disconnects are serialized and stored in the database
 * as a `Photocard`.
 *
 * This model contains only the minimal information required to restore and resume
 * the act at a later time.
 *
 * @property actId Unique identifier of this stage act.
 * @property name Identifier of the associated stage act (typically [StageAct.name]).
 * @property progress Current progress state of the stage act.
 * @property data Key-value data used to distinguish multiple acts of the same name.
 */
@Serializable
data class Photocard(
    val actId: String,
    val name: String,
    val progress: ActProgress,
    val data: Map<String, String>
)
