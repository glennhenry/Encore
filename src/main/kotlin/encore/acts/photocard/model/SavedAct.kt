package encore.acts.photocard.model

import encore.acts.StageAct
import encore.datastore.collection.PlayerId
import kotlinx.serialization.Serializable

/**
 * Holds all persistable [StageAct] of [playerId].
 *
 * @property playerId The associated player.
 * @property photocards Every photocards instance.
 */
@Serializable
data class SavedAct(
    val playerId: PlayerId,
    val photocards: List<Photocard>
)
