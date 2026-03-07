package data.collection

import core.data.AdminData
import kotlinx.serialization.Serializable
import user.model.ServerMetadata
import user.model.UserProfile

/**
 * Database-level representation of a player's account data.
 */
@Serializable
data class PlayerAccount(
    val playerId: String,
    val hashedPassword: String,
    val profile: UserProfile,
    val metadata: ServerMetadata
) {
    companion object {
        fun admin(): PlayerAccount {
            return PlayerAccount(
                playerId = AdminData.PLAYER_ID,
                hashedPassword = AdminData.HASHED_PASSWORD,
                profile = UserProfile.admin(),
                metadata = ServerMetadata()
            )
        }

        /**
         * Create a fake account where password is [AdminData.PASSWORD].
         */
        fun fake(playerId: String, username: String): PlayerAccount {
            return PlayerAccount(
                playerId = playerId,
                hashedPassword = AdminData.HASHED_PASSWORD,
                profile = UserProfile.default(playerId, username),
                metadata = ServerMetadata()
            )
        }
    }
}
