package encore.datastore

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import encore.datastore.collection.PlayerAccount
import encore.datastore.collection.PlayerId
import encore.datastore.collection.PlayerObjects
import encore.datastore.collection.PlayerServerObjects
import encore.datastore.collection.ServerObjects
import encore.fancam.Fancam
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.measureTime

data class MongoCollectionName(
    val playerAccount: String,
    val playerObjects: String,
    val playerServerObjects: String,
    val serverObjects: String
)

/**
 * Implementation of [DataStore] based on the MongoDB database.
 *
 * Uses Kotlin MongoDB coroutine driver.
 */
class MongoDataStore(db: MongoDatabase, collectionName: MongoCollectionName) : DataStore {
    private val accounts = db.getCollection<PlayerAccount>(collectionName.playerAccount)
    private val playerObjects = db.getCollection<PlayerObjects>(collectionName.playerObjects)
    private val playerServerObjects = db.getCollection<PlayerServerObjects>(collectionName.playerServerObjects)
    private val serverObjects = db.getCollection<ServerObjects>(collectionName.serverObjects)

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
            prepareServerObjects()
            setupIndexes()
        } catch (e: Exception) {
            Fancam.error { "MongoDB failed during initialization: $e" }
        }
    }

    private suspend fun setupIndexes() {
        serverObjects.createIndex(Indexes.text())
        Fancam.info { "Mongo index set up" }
    }

    private suspend fun prepareServerObjects() {
        when (val count = serverObjects.estimatedDocumentCount()) {
            0L -> {
                serverObjects.insertOne(ServerObjects())
            }

            1L -> return

            else -> {
                Fancam.warn { "Detected multiple server object document count=$count" }
            }
        }
    }

    override suspend fun playerExists(playerId: PlayerId): Boolean {
        return accounts.find(Filters.eq(FieldPlayerId, playerId)).firstOrNull() != null
    }

    override suspend fun getPlayerAccount(playerId: PlayerId): PlayerAccount? {
        return accounts.find(Filters.eq(FieldPlayerId, playerId)).firstOrNull()
    }

    override suspend fun getPlayerObjects(playerId: PlayerId): PlayerObjects? {
        return playerObjects.find(Filters.eq(FieldPlayerId, playerId)).firstOrNull()
    }

    override suspend fun getPlayerServerObjects(playerId: PlayerId): PlayerServerObjects? {
        return playerServerObjects.find(Filters.eq(FieldPlayerId, playerId)).firstOrNull()
    }

    override suspend fun getServerObjects(): ServerObjects {
        return serverObjects.find(Filters.eq("dbId", "sobjs")).firstOrNull()
            ?: throw NoSuchElementException("ServerObjects not found, please ensure ServerObjects creation.")
    }

    override suspend fun create(
        account: PlayerAccount,
        playerObjects: PlayerObjects,
        playerServerObjects: PlayerServerObjects
    ): Result<Unit> {
        return try {
            val accountAck = accounts.insertOne(account).wasAcknowledged()
            val pObjAck = this.playerObjects.insertOne(playerObjects).wasAcknowledged()
            val psObjAck = this.playerServerObjects.insertOne(playerServerObjects).wasAcknowledged()

            if (accountAck && pObjAck && psObjAck) {
                Result.success(Unit)
            } else {
                Fancam.error {
                    "MongoDB creation not acknowledged: playerId=${account.playerId}, accountAck=$accountAck, pObjAck=$pObjAck, psObjAck=$psObjAck"
                }
                Result.failure(
                    IllegalStateException("MongoDB insert not acknowledged")
                )
            }
        } catch (e: Exception) {
            Fancam.error(e) { "MongoDB creation failed: playerId=${account.playerId}" }
            Result.failure(e)
        }
    }

    override suspend fun delete(playerId: PlayerId): Result<Unit> {
        return try {
            val accountAck = accounts.deleteOne(Filters.eq(FieldPlayerId, playerId)).wasAcknowledged()
            val pObjAck = playerObjects.deleteOne(Filters.eq(FieldPlayerId, playerId)).wasAcknowledged()
            val psObjAck = playerServerObjects.deleteOne(Filters.eq(FieldPlayerId, playerId)).wasAcknowledged()

            if (accountAck && pObjAck && psObjAck) {
                Result.success(Unit)
            } else {
                Fancam.error { "MongoDB deletion not acknowledged: playerId=$playerId, accountAck=$accountAck, pObjAck=$pObjAck, psObjAck=$psObjAck" }
                Result.failure(IllegalStateException("MongoDB deletion not acknowledged"))
            }
        } catch (e: Exception) {
            Fancam.error(e) { "MongoDB deletion failed: playerId=$playerId" }
            Result.failure(e)
        }
    }

    override suspend fun shutdown() = Unit
}
