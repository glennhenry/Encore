package encore.datastore

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import encore.datastore.collection.PlayerAccount
import encore.datastore.collection.PlayerObjects
import encore.datastore.collection.ServerObjects
import encore.fancam.Fancam
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.measureTime

const val MONGO_PLAYER_ACCOUNT_COLLECTION_NAME = "player_account"
const val MONGO_PLAYER_OBJECTS_COLLECTION_NAME = "player_objects"
const val MONGO_SERVER_OBJECTS_COLLECTION_NAME = "server_objects"

/**
 * Implementation of [DataStore] based on the MongoDB database.
 *
 * Uses Kotlin MongoDB coroutine driver.
 */
class MongoDataStore(db: MongoDatabase) : DataStore {
    private val accounts = db.getCollection<PlayerAccount>(MONGO_PLAYER_ACCOUNT_COLLECTION_NAME)
    private val playerObjects = db.getCollection<PlayerObjects>(MONGO_PLAYER_OBJECTS_COLLECTION_NAME)
    private val serverObjects = db.getCollection<ServerObjects>(MONGO_SERVER_OBJECTS_COLLECTION_NAME)

    private val initJob = CoroutineScope(Dispatchers.IO).async { setupCollections() }

    override suspend fun awaitInit() {
        Fancam.info { "Waiting for MongoDB initialization..." }
        val elapsed = measureTime {
            initJob.await()
        }
        Fancam.info { "MongoDB completed initialization ($elapsed)" }
    }

    private suspend fun setupCollections() {
        try {
            val count = accounts.estimatedDocumentCount()
            Fancam.info { "MongoDB contains $count accounts." }
            setupIndexes()
        } catch (e: Exception) {
            Fancam.error { "MongoDB failed during initialization: $e" }
        }
    }

    suspend fun setupIndexes() {
        accounts.createIndex(Indexes.text("profile.displayName"))
        Fancam.info { "Indexes created" }
    }

    override suspend fun playerExists(playerId: String): Boolean {
        return accounts.find(Filters.eq("playerId", playerId)).firstOrNull() != null
    }

    override suspend fun getPlayerAccount(playerId: String): PlayerAccount? {
        return accounts.find(Filters.eq("playerId", playerId)).firstOrNull()
    }

    override suspend fun getPlayerObjects(playerId: String): PlayerObjects? {
        return playerObjects.find(Filters.eq("playerId", playerId)).firstOrNull()
    }

    override suspend fun getServerObjects(): ServerObjects {
        return serverObjects.find(Filters.eq("dbId", "sobjs")).firstOrNull()
            ?: throw NoSuchElementException("ServerObjects not found, please ensure ServerObjects creation.")
    }

    override suspend fun create(account: PlayerAccount, objects: PlayerObjects): Result<Unit> {
        return try {
            val accountAck = accounts.insertOne(account).wasAcknowledged()
            val objectsAck = playerObjects.insertOne(objects).wasAcknowledged()

            if (accountAck && objectsAck) {
                Result.success(Unit)
            } else {
                Fancam.error {
                    "MongoDB creation not acknowledged: playerId=${account.playerId}, accountAck=$accountAck, objectsAck=$objectsAck"
                }
                Result.failure(
                    IllegalStateException("MongoDB insert not acknowledged")
                )
            }
        } catch (e: Exception) {
            Fancam.error { "MongoDB creation failed: playerId=${account.playerId}, error=$e" }
            Result.failure(e)
        }
    }

    override suspend fun delete(playerId: String): Result<Unit> {
        return try {
            val deleteAck = accounts.deleteOne(Filters.eq("playerId", playerId)).wasAcknowledged()
            if (deleteAck) {
                Result.success(Unit)
            } else {
                Fancam.error { "MongoDB deletion not acknowledged: playerId=$playerId" }
                Result.failure(IllegalStateException("MongoDB deletion not acknowledged"))
            }
        } catch (e: Exception) {
            Fancam.error { "MongoDB deletion failed: playerId=$playerId, error=$e" }
            Result.failure(e)
        }
    }

    override suspend fun shutdown() = Unit
}
