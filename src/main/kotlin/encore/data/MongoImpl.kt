package encore.data

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes
import com.mongodb.client.result.UpdateResult
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.toxicbakery.bcrypt.Bcrypt
import encore.core.data.AdminData
import encore.data.collection.PlayerAccount
import encore.data.collection.PlayerData
import encore.data.collection.ServerData
import io.ktor.util.date.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import encore.user.model.ServerMetadata
import encore.user.model.UserProfile
import encore.utils.UUID
import encore.fancam.Fancam
import kotlin.io.encoding.Base64

/**
 * Implementation of [Database] using MongoDB.
 */
class MongoImpl(db: MongoDatabase, private val adminEnabled: Boolean) : Database {
    private val accountCollection = db.getCollection<PlayerAccount>("player_account")
    private val dataCollection = db.getCollection<PlayerData>("player_data")
    private val serverCollection = db.getCollection<ServerData>("server_data")

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getCollection(name: String): MongoCollection<T> {
        return when (name) {
            "player_account" -> accountCollection
            "player_data" -> dataCollection
            "server_data" -> serverCollection
            else -> {
                throw IllegalArgumentException("Invalid collection name: $name")
            }
        } as MongoCollection<T>
    }

    init {
        Fancam.info { "Initializing MongoDB..." }
        CoroutineScope(Dispatchers.IO).launch {
            setupCollections()
        }
    }

    private suspend fun setupCollections() {
        try {
            val count = accountCollection.estimatedDocumentCount()
            Fancam.info { "MongoDB: User collection ready, contains $count users." }
            if (adminEnabled) {
                val adminDoc = accountCollection.find(Filters.eq("playerId", AdminData.PLAYER_ID)).firstOrNull()
                if (adminDoc == null) {
                    val start = getTimeMillis()
                    val doc = PlayerAccount.admin()
                    val obj = PlayerData.admin()

                    accountCollection.insertOne(doc)
                    dataCollection.insertOne(obj)

                    Fancam.info { "MongoDB: Admin enabled, account inserted in ${getTimeMillis() - start}ms" }
                } else {
                    Fancam.info { "MongoDB: Admin enabled, account already exists." }
                }
            }
            setupIndexes()
        } catch (e: Exception) {
            Fancam.error { "MongoDB: Failed during setupCollections: $e" }
        }
    }

    suspend fun setupIndexes() {
        accountCollection.createIndex(Indexes.text("profile.displayName"))
    }

    override suspend fun loadPlayerAccount(playerId: String): PlayerAccount? {
        return accountCollection.find(Filters.eq("playerId", playerId)).firstOrNull()
    }

    override suspend fun loadPlayerData(playerId: String): PlayerData? {
        return dataCollection.find(Filters.eq("playerId", playerId)).firstOrNull()
    }

    override suspend fun loadServerData(): ServerData {
        return serverCollection.find().firstOrNull() ?: throw NoSuchElementException("No global ServerData found")
    }

    override suspend fun createPlayer(username: String, password: String): String {
        val playerId = UUID.new()
        val profile = UserProfile.default(playerId, username)

        val doc = PlayerAccount(
            playerId = playerId,
            hashedPassword = hashPw(password),
            profile = profile,
            metadata = ServerMetadata()
        )
        val obj = PlayerData.newGame(playerId)

        accountCollection.insertOne(doc)
        dataCollection.insertOne(obj)

        return playerId
    }

    private fun hashPw(password: String): String {
        return Base64.encode(Bcrypt.hash(password, 10))
    }

    override suspend fun shutdown() = Unit
}

/**
 * Executes the given [block] that returns a value of type [T] or null.
 *
 * - If [block] returns null, this will return a failed [Result] containing a [NoSuchElementException]
 *   with the provided [nullMessage].
 * - If [block] throws any exception, it will be caught and wrapped in a failed [Result].
 * - Otherwise, the returned non-null value is wrapped in a successful [Result].
 *
 * You need to throw exception explicitly if there are multiple exception messages.
 */
inline fun <T> runMongoCatching(
    nullMessage: String = "Document not found",
    block: () -> T?
): Result<T> {
    return try {
        val value = block()
        if (value == null) {
            Result.failure(NoSuchElementException(nullMessage))
        } else {
            Result.success(value)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * Executes the given [block] that returns a [Boolean] indicating success.
 *
 * - If [block] returns false, this will return a failed [Result] containing a [NoSuchElementException]
 *   with the provided [failMessage].
 * - If [block] throws any exception, it will be caught and wrapped in a failed [Result].
 * - If [block] returns true, a successful [Result] with [Unit] is returned.
 *
 * You need to throw exception explicitly if there are multiple exception messages.
 */
inline fun runMongoCatchingUnit(
    failMessage: String = "Document not found",
    block: () -> Boolean
): Result<Unit> {
    return try {
        if (block()) Result.success(Unit)
        else Result.failure(NoSuchElementException(failMessage))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * Check whether update operation were matched and modified; and throw an error
 * if it's not.
 *
 * @param [playerId] The DB operation for specific [playerId].
 * @param info Additional info added to error message.
 *             Typically, add function or class name, or context about the operation
 * @throws NoSuchElementException if `matchedCount` is less than 1
 * @throws IllegalStateException if `modifiedCount` is less than 1
 */
fun UpdateResult.throwIfNotModified(playerId: String, info: String = "") {
    if (matchedCount < 1) {
        throw NoSuchElementException("playerId=$playerId not found; $info")
    }
    if (modifiedCount < 1) {
        throw IllegalStateException("Failed to update one for playerId=$playerId; $info")
    }
}
