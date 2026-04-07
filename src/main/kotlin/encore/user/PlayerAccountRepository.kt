package encore.user

import encore.datastore.collection.PlayerAccount
import encore.datastore.collection.PlayerId

/**
 * Repository for [PlayerAccount].
 */
interface PlayerAccountRepository {
    suspend fun doesUsernameExist(username: String): Result<Boolean>
    suspend fun isUsernameAvailable(username: String): Result<Boolean>
    suspend fun doesEmailExist(email: String): Result<Boolean>
    suspend fun isEmailAvailable(email: String): Result<Boolean>

    suspend fun getPlayerAccountByName(username: String): Result<PlayerAccount>
    suspend fun getPlayerAccountById(playerId: PlayerId): Result<PlayerAccount>
    suspend fun getPlayerIdFromName(username: String): Result<String>

    suspend fun updatePlayerAccount(playerId: PlayerId, account: PlayerAccount): Result<Unit>
    suspend fun updateLastLogin(playerId: PlayerId, lastLogin: Long): Result<Unit>

    /**
     * Confirm the player's password matches the stored credentials for the given username
     *
     * @return The associated `playerId` if correct.
     *         Returns `null` if account don't exist or password is wrong.
     */
    suspend fun verifyCredentials(username: String, password: String): Result<String>
}
