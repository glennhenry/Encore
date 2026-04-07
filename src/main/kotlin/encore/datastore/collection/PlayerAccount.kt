package encore.datastore.collection

import kotlinx.serialization.Serializable
import encore.user.model.ServerMetadata
import encore.user.model.Profile

/**
 * Representation of a player's account.
 *
 * This model is used to represent a player account in the database.
 *
 * @property playerId Unique identifier of the player.
 * @property username Display name of the player.
 * @property email Email address associated with this account.
 * @property hashedPassword Hashed version of the account's password.
 * @property profile Representation of the player's profile.
 * @property metadata Extra information of the player.
 */
@Serializable
data class PlayerAccount(
    val playerId: String,
    val username: String,
    val email: String,
    val hashedPassword: String,
    val profile: Profile,
    val metadata: ServerMetadata
)
