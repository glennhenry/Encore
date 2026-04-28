package encore.account

import encore.datastore.collection.PlayerAccount
import encore.datastore.collection.PlayerId
import encore.account.model.Credentials
import encore.account.model.Profile
import encore.repository.Repository

/**
 * Repository template for [PlayerAccount].
 *
 * Implementation should abstract the data access to player's account.
 * For instance:
 * - Mongo implementation provides API to the underlying `MongoCollection`
 * - SQL implementation provides API to the account table.
 * - In-memory implementation provides API to the in-memory data representation.
 *
 * Each operation should return a [Result] type to denote the outcome.
 * [Result.failure] is used when the operation fails due to an internal failure
 * like DB errors and not business outcome.
 */
interface AccountRepository: Repository {
    /**
     * Returns [PlayerAccount] associated with the given [playerId], if it exists.
     *
     * Returns [Result.success] with:
     * - the [PlayerAccount] if found
     * - `null` if no account exists for the given [playerId]
     *
     * Returns [Result.failure] if an error occurs while retrieving the data.
     */
    suspend fun getAccountByPlayerId(playerId: PlayerId): Result<PlayerAccount?>

    /**
     * Returns [PlayerAccount] associated with the given [username], if it exists.
     *
     * Returns [Result.success] with:
     * - the [PlayerAccount] if found
     * - `null` if no account exists for the given [username]
     *
     * Returns [Result.failure] if an error occurs while retrieving the data.
     */
    suspend fun getAccountByUsername(username: String): Result<PlayerAccount?>

    /**
     * Returns [PlayerId] associated with the given [username], if it exists.
     *
     * Returns [Result.success] with:
     * - the [PlayerId] if found
     * - `null` if no account exists for the given [username]
     *
     * Returns [Result.failure] if an error occurs while retrieving the data.
     */
    suspend fun getPlayerIdByUsername(username: String): Result<PlayerId?>

    /**
     * Returns the [Credentials] of the provided [username].
     */
    suspend fun getCredentials(username: String): Result<Credentials?>

    /**
     * Update [PlayerAccount] of [playerId] with the new [account].
     * @return [Result] type denoting success or failure.
     */
    suspend fun updatePlayerAccount(playerId: PlayerId, account: PlayerAccount): Result<Unit>

    /**
     * Update [Profile] of [playerId] with the new [profile].
     * @return [Result] type denoting success or failure.
     */
    suspend fun updateProfile(playerId: PlayerId, profile: Profile): Result<Unit>

    /**
     * Update [Profile.lastActiveAt] of [playerId] with [lastActivity].
     * @return [Result] type denoting success or failure.
     */
    suspend fun updateLastActivity(playerId: PlayerId, lastActivity: Long): Result<Unit>

    /**
     * Returns whether the provided [username] already exists.
     *
     * Returns [Result.success] with:
     * - [Result.value]` = true` if username exists
     * - [Result.value]` = false` if username does not exist
     *
     * Returns [Result.failure] if an error occurs while retrieving the data.
     */
    suspend fun usernameExists(username: String): Result<Boolean>

    /**
     * Returns whether the provided [email] already exists.
     *
     * Returns [Result.success] with:
     * - [Result.value]` = true` if email exists
     * - [Result.value]` = false` if email does not exist
     *
     * Returns [Result.failure] if an error occurs while retrieving the data.
     */
    suspend fun emailExists(email: String): Result<Boolean>
}
