package encore.account

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import encore.datastore.collection.FieldPlayerId
import encore.datastore.collection.PlayerAccount
import encore.datastore.collection.PlayerId
import encore.datastore.runMongoCatching
import encore.datastore.throwIfNotModified
import encore.account.model.Credentials
import encore.account.model.Profile
import encore.utils.then
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document

/**
 * MongoDB implementation of [AccountRepository].
 */
class MongoAccountRepository(val accountCollection: MongoCollection<PlayerAccount>) : AccountRepository {
    override suspend fun getAccountByPlayerId(playerId: PlayerId): Result<PlayerAccount> {
        return runMongoCatching {
            accountCollection
                .find(Filters.eq(FieldPlayerId, playerId))
                .firstOrNull()
        }
    }

    override suspend fun getAccountByUsername(username: String): Result<PlayerAccount> {
        return runMongoCatching {
            accountCollection
                .find(Filters.eq(PlayerAccount::username.name, username))
                .firstOrNull()
        }
    }

    override suspend fun getPlayerIdByUsername(username: String): Result<PlayerId> {
        return runMongoCatching {
            accountCollection
                .find(Filters.eq(PlayerAccount::username.name, username))
                .firstOrNull()
                ?.playerId
        }
    }

    override suspend fun getCredentials(username: String): Result<Credentials?> {
        return runMongoCatching {
            val account = accountCollection
                .withDocumentClass<Document>()
                .find(Filters.eq(PlayerAccount::username.name, username))
                .projection(Projections.include(PlayerAccount::hashedPassword.name, FieldPlayerId))
                .firstOrNull()

            if (account == null) {
                return Result.success(null)
            }

            val playerId = account.getString(FieldPlayerId)
            val hashedPassword = account.getString(PlayerAccount::hashedPassword.name)
            return Result.success(Credentials(playerId, hashedPassword))
        }
    }

    override suspend fun updatePlayerAccount(
        playerId: PlayerId,
        account: PlayerAccount
    ): Result<Unit> {
        return runMongoCatching {
            accountCollection
                .replaceOne(Filters.eq(FieldPlayerId, playerId), account)
                .throwIfNotModified("updatePlayerAccount not updated for $playerId")
        }
    }

    override suspend fun updateProfile(
        playerId: PlayerId,
        profile: Profile
    ): Result<Unit> {
        return runMongoCatching {
            accountCollection
                .updateOne(
                    Filters.eq(FieldPlayerId, playerId),
                    Updates.set(PlayerAccount::profile.name, profile)
                )
                .throwIfNotModified("updateProfile not updated for $playerId")
        }
    }

    override suspend fun updateLastActivity(
        playerId: PlayerId,
        lastActivity: Long
    ): Result<Unit> {
        return runMongoCatching {
            accountCollection
                .updateOne(
                    Filters.eq(FieldPlayerId, playerId),
                    Updates.set(
                        PlayerAccount::profile.name.then(Profile::lastActiveAt.name),
                        lastActivity
                    )
                )
                .throwIfNotModified("updateLastActivity not updated for $playerId")
        }
    }

    override suspend fun usernameExists(username: String): Result<Boolean> {
        return runMongoCatching {
            accountCollection
                .find(Filters.eq(PlayerAccount::username.name, username))
                .projection(null)
                .firstOrNull() != null
        }
    }

    override suspend fun emailExists(email: String): Result<Boolean> {
        return runMongoCatching {
            accountCollection
                .find(Filters.eq(PlayerAccount::email.name, email))
                .projection(null)
                .firstOrNull() != null
        }
    }
}
