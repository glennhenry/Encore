package encore.account.model

import encore.datastore.collection.PlayerId
import kotlinx.serialization.Serializable

/**
 * Represent the required data to access an account.
 *
 * @property playerId [PlayerId] associated with this credentials.
 * @property hashedPassword the hashed version of account's password.
 */
@Serializable
data class Credentials(
    val playerId: PlayerId,
    val hashedPassword: String
)
