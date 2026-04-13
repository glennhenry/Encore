package encore.user

import encore.datastore.collection.PlayerAccount
import encore.datastore.collection.PlayerId
import encore.user.model.Credentials
import encore.user.model.Profile

/**
 * Blank implementation (no operation) of [AccountRepository] only used for testing purposes.
 */
class BlankAccountRepository : AccountRepository {
    override suspend fun getAccountByPlayerId(playerId: PlayerId): Result<PlayerAccount?> = TODO("NO OPERATION")
    override suspend fun getAccountByUsername(username: String): Result<PlayerAccount?> = TODO("NO OPERATION")
    override suspend fun getPlayerIdByUsername(username: String): Result<PlayerId?> = TODO("NO OPERATION")
    override suspend fun getCredentials(username: String): Result<Credentials?> = TODO("NO OPERATION")
    override suspend fun updatePlayerAccount(playerId: PlayerId, account: PlayerAccount): Result<Unit> = TODO("NO OPERATION")
    override suspend fun updateProfile(playerId: PlayerId, profile: Profile): Result<Unit> = TODO("NO OPERATION")
    override suspend fun usernameExists(username: String): Result<Boolean> = TODO("NO OPERATION")
    override suspend fun emailExists(email: String): Result<Boolean> = TODO("NO OPERATION")
}
