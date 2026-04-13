package encore.user

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.toxicbakery.bcrypt.Bcrypt
import encore.datastore.collection.PlayerAccount
import encore.datastore.collection.PlayerId
import encore.datastore.runMongoCatching
import encore.datastore.throwIfNotModified
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import kotlin.io.encoding.Base64

class MongoAccountRepository(val accountCollection: MongoCollection<PlayerAccount>) : AccountRepository {
    override suspend fun getPlayerAccountByName(username: String): Result<PlayerAccount> {
        return runMongoCatching {
            accountCollection
                .find(Filters.eq("username", username))
                .projection(null)
                .firstOrNull()
        }
    }

    override suspend fun getPlayerAccountById(playerId: PlayerId): Result<PlayerAccount> {
        return runMongoCatching {
            accountCollection
                .find(Filters.eq("playerId", playerId))
                .projection(null)
                .firstOrNull()
        }
    }

    override suspend fun getPlayerIdFromName(username: String): Result<String> {
        return runMongoCatching {
            accountCollection
                .find(Filters.eq("username", username))
                .projection(null)
                .firstOrNull()
                ?.playerId
        }
    }

    override suspend fun doesUsernameExist(username: String): Result<Boolean> {
        return runMongoCatching {
            accountCollection
                .find(Filters.eq("username", username))
                .projection(null)
                .firstOrNull() != null
        }
    }

    override suspend fun isUsernameAvailable(username: String): Result<Boolean> {
        return runMongoCatching {
            accountCollection
                .find(Filters.eq("username", username))
                .projection(null)
                .firstOrNull() == null
        }
    }

    override suspend fun doesEmailExist(email: String): Result<Boolean> {
        return runMongoCatching {
            accountCollection
                .find(Filters.eq("email", email))
                .projection(null)
                .firstOrNull() != null
        }
    }

    override suspend fun isEmailAvailable(email: String): Result<Boolean> {
        return runMongoCatching {
            accountCollection
                .find(Filters.eq("email", email))
                .projection(null)
                .firstOrNull() == null
        }
    }

    override suspend fun updatePlayerAccount(
        playerId: PlayerId,
        account: PlayerAccount
    ): Result<Unit> {
        return runMongoCatching {
            val result = accountCollection.replaceOne(Filters.eq("playerId", playerId), account)
            result.throwIfNotModified("updatePlayerAccount $playerId")
        }
    }

    override suspend fun updateLastLogin(playerId: PlayerId, lastLogin: Long): Result<Unit> {
        return runMongoCatching {
            val result = accountCollection.updateOne(
                Filters.eq("playerId", playerId),
                Updates.set("profile.lastLogin", lastLogin)
            )
            result.throwIfNotModified("updateLastLogin $playerId")
        }
    }

    override suspend fun verifyCredentials(username: String, password: String): Result<String> {
        return runMongoCatching {
            val doc = accountCollection
                .withDocumentClass<Document>()
                .find(Filters.eq("username", username))
                .projection(Projections.include("hashedPassword", "playerId"))
                .firstOrNull()

            if (doc == null) return@runMongoCatching null

            val hashed = doc.getString("hashedPassword")
            val playerId = doc.getString("playerId")
            val matches = Bcrypt.verify(password, Base64.decode(hashed))

            if (matches) playerId else null
        }
    }
}
