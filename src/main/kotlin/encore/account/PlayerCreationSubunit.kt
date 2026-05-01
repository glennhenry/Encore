package encore.account

import encore.account.model.Profile
import encore.datastore.BlankDataStore
import encore.datastore.DataStore
import encore.datastore.collection.*
import encore.fancam.Fancam
import encore.subunit.Subunit
import encore.subunit.scope.ServerScope
import encore.utils.Ids
import encore.utils.hash
import game.AdminData
import io.ktor.util.date.*

/**
 * Server-scoped subunit responsible for player creation.
 *
 * This should handle the logic to create a player, which typically involves
 * inserting default data to the three base collections: [PlayerAccount],
 * [PlayerObjects], and [PlayerServerObjects].
 *
 * This subunit doesn't handle server data update in [ServerObjects]
 * for the player. This should be handled separately via external orchestration
 * (e.g., in the registration).
 *
 * Requires a [DataStore] to persist the newly created players.
 */
class PlayerCreationSubunit(private val dataStore: DataStore) : Subunit<ServerScope> {
    /**
     * Create a player account with the specified [username], [password], and [email].
     *
     * Email is optional and will be defaulted to username@email.com
     *
     * @return [PlayerId] of the newly created player
     * @throws [Throwable] an exception type from the underlying datastore or
     *         [IllegalStateException] when the account creation failed without any exception passed.
     */
    suspend fun createPlayer(
        username: String, password: String,
        email: String = "$username@email.com"
    ): PlayerId {
        val playerId = Ids.uuid()

        val account = PlayerAccount(
            playerId = playerId,
            username = username,
            email = email,
            hashedPassword = hash(password),
            profile = defaultProfile(playerId),
        )
        val pObj = PlayerObjects.newGame(playerId)
        val psObj = PlayerServerObjects.newGame(playerId)

        val result = dataStore.create(account, pObj, psObj)
        if (result.isSuccess) {
            return playerId
        }

        Fancam.error { "Account creation failed for $username" }

        throw result.exceptionOrNull()
            ?: IllegalStateException("Account creation failed with unknown error (exception was null)")
    }

    /**
     * Create a reserved admin account if it doesn't exist.
     *
     * @param alwaysRecreate Whether to always recreate the account.
     * @throws [Throwable] an exception type from the underlying datastore or
     *         [IllegalStateException] when the account creation failed without any exception passed.
     */
    suspend fun createAdmin(adminData: AdminData, alwaysRecreate: Boolean = false) {
        if (alwaysRecreate) {
            dataStore.delete(adminData.PLAYER_ID)
        } else if (dataStore.playerExists(adminData.PLAYER_ID)) {
            Fancam.info { "Ignoring admin account creation (already exists)" }
            return
        }

        val account = PlayerAccount(
            playerId = AdminData.PLAYER_ID,
            username = AdminData.USERNAME,
            email = AdminData.EMAIL,
            hashedPassword = AdminData.HASHED_PASSWORD,
            profile = defaultProfile(AdminData.PLAYER_ID),
        )
        val pObj = PlayerObjects.admin()
        val psObj = PlayerServerObjects.admin()

        val result = dataStore.create(account, pObj, psObj)

        if (result.isSuccess) {
            Fancam.info { "New admin account created" }
        } else {
            Fancam.error {
                "Admin account creation failed, reason: ${result.exceptionOrNull()}"
            }

            throw result.exceptionOrNull()
                ?: IllegalStateException("Admin account creation failed with unknown error (exception was null)")
        }
    }

    private fun defaultProfile(playerId: PlayerId): Profile {
        val now = getTimeMillis()
        return Profile(
            playerId = playerId,
            createdAt = now,
            lastActiveAt = now
        )
    }

    // unused
    override suspend fun debut(scope: ServerScope): Result<Unit> = Result.success(Unit)
    override suspend fun disband(scope: ServerScope): Result<Unit> = Result.success(Unit)

    companion object {
        /**
         * Creates a test instance of [PlayerCreationSubunit].
         *
         * @param dataStore dependency for persistence.
         * Use [BlankDataStore] when the behavior is not relevant to the test.
         */
        fun createForTest(dataStore: DataStore = BlankDataStore()): PlayerCreationSubunit {
            return PlayerCreationSubunit(dataStore)
        }
    }
}
