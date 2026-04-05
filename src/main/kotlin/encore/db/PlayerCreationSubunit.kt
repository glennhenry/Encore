package encore.db

import com.toxicbakery.bcrypt.Bcrypt
import encore.db.collection.PlayerAccount
import encore.db.collection.PlayerObjects
import encore.fancam.Fancam
import encore.subunit.Subunit
import encore.subunit.scope.ServerScope
import encore.user.AdminData
import encore.user.model.ServerMetadata
import encore.user.model.UserProfile
import encore.utils.UUID
import kotlin.io.encoding.Base64

/**
 * Server-scoped subunit responsible for player creation.
 *
 * Requires a [DataStore] to persist the newly created players.
 */
class PlayerCreationSubunit(private val dataStore: DataStore) : Subunit<ServerScope> {
    /**
     * Create a player account with the specified [username] and [password].
     *
     * @return playerId of the newly created player
     * @throws [Throwable] an exception type from the underlying datastore or
     *         [IllegalStateException] when the account creation failed without any exception passed.
     *
     */
    suspend fun createPlayer(username: String, password: String): String {
        val playerId = UUID.new()
        val profile = UserProfile.default(playerId, username)

        val account = PlayerAccount(
            playerId = playerId,
            hashedPassword = hashPw(password),
            profile = profile,
            metadata = ServerMetadata()
        )
        val objects = PlayerObjects.newGame(playerId)

        val result = dataStore.create(account, objects)
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
     */
    suspend fun createAdmin(adminData: AdminData, alwaysRecreate: Boolean = false) {
        if (alwaysRecreate) {
            dataStore.delete(adminData.PLAYER_ID)
        } else if (dataStore.playerExists(adminData.PLAYER_ID)) {
            Fancam.info { "Ignoring admin account creation (already exists)" }
            return
        }

        val result = dataStore.create(PlayerAccount.admin(), PlayerObjects.admin())

        if (result.isSuccess) {
            Fancam.info { "New admin account created" }
        } else {
            Fancam.error {
                "Admin account creation failed, reason: ${result.exceptionOrNull()}"
            }
        }
    }

    private fun hashPw(password: String): String {
        return Base64.encode(Bcrypt.hash(password, 10))
    }

    // unused
    override suspend fun debut(scope: ServerScope): Result<Unit> = Result.success(Unit)
    override suspend fun disband(scope: ServerScope): Result<Unit> = Result.success(Unit)
}
