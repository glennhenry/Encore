package encore.activity

import encore.datastore.collection.PlayerId

/**
 * Represents the current online status of a player.
 *
 * @property onlineSince The timestamp (in milliseconds since epoch)
 *                       when the player came online.
 * @property lastNetworkActivity The timestamp (in milliseconds since epoch)
 *                               of the player's most recent network activity.
 */
data class PlayerActivity(
    val playerId: PlayerId,
    val onlineSince: Long,
    val lastNetworkActivity: Long,
)
