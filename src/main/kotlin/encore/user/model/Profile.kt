package encore.user.model

import encore.datastore.collection.PlayerId
import kotlinx.serialization.Serializable

/**
 * Server-owned information about a player's profile.
 *
 * This would include any server-related information such as
 * country, avatar, locale, and not game-specific information
 * like player's ranking or status.
 *
 * @property playerId Unique identifier of the player.
 * @property createdAt Epoch millis of the account creation date.
 * @property lastActiveAt Epoch millis of the account last activity.
 */
@Serializable
data class Profile(
    val playerId: PlayerId,
    val createdAt: Long,
    val lastActiveAt: Long,
)
