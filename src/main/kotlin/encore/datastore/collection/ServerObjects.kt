package encore.datastore.collection

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
 */
data class ServerObjects(
    val dbId: String = "sobjs",
)
