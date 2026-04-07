package encore.user

import encore.datastore.collection.PlayerAccount
import encore.datastore.collection.PlayerId

/**
 * Empty implementation (no operation) of [PlayerAccountRepository] only used for testing purposes.
 */
class EmptyPlayerAccountRepository : PlayerAccountRepository {
    override suspend fun doesUsernameExist(username: String): Result<Boolean> = TODO("ONLY TEST")
    override suspend fun isUsernameAvailable(username: String): Result<Boolean> = TODO("ONLY TEST")
    override suspend fun doesEmailExist(email: String): Result<Boolean> = TODO("ONLY TEST")
    override suspend fun isEmailAvailable(email: String): Result<Boolean>  = TODO("ONLY TEST")
    override suspend fun getPlayerAccountByName(username: String): Result<PlayerAccount> = TODO("ONLY TEST")
    override suspend fun getPlayerAccountById(playerId: PlayerId): Result<PlayerAccount> = TODO("ONLY TEST")
    override suspend fun getPlayerIdFromName(username: String): Result<String> = TODO("ONLY TEST")
    override suspend fun updatePlayerAccount(playerId: PlayerId, account: PlayerAccount): Result<Unit> = TODO("ONLY TEST")
    // method called in test
    override suspend fun updateLastLogin(playerId: PlayerId, lastLogin: Long): Result<Unit> = Result.success(Unit)
    override suspend fun verifyCredentials(username: String, password: String): Result<String> = TODO("ONLY TEST")
}
