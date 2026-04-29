package encore.datastore.collection

import encore.acts.photocard.model.Photocard
import encore.acts.photocard.model.SavedAct
import kotlinx.serialization.Serializable

/**
 * Represents server-wide data.
 *
 * This model contains global, non–player-specific information.
 * It may include both gameplay-related data (e.g., leaderboards, clans)
 * and non-game or operational data (e.g., news, analytics).
 *
 * This object is intended to be a singleton within its collection.
 *
 * @property dbId Unique identifier. Since only one instance of `ServerObjects`
 *                exists, this acts as a fixed key.
 * @property acts Contains saved acts for each players.
 * @property serverActs Contains saved acts which is server-owned.
 */
@Serializable
data class ServerObjects(
    val dbId: String = ServerObjectsId,
    val acts: List<SavedAct> = emptyList(),
    val serverActs: List<Photocard> = emptyList()
)

const val ServerObjectsId = "sobjs"

/**
 * Represent the **one and only** unique identifier for the server.
 *
 * This is used for identification in things like:
 * - [ActScope.ownerId]
 */
const val ServerId = "Xiaoting <33"
