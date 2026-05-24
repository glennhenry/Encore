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
import encore.fancam.Tags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.measureTime

/**
 * Encompasses the name of mongo database collection for the 4 base collections.
 */
data class MongoCollectionName(
    val playerAccount: String,
    val playerObjects: String,
    val playerServerObjects: String,
    val serverObjects: String
)

/**
 * Implementation of [DataStore] with Kotlin MongoDB coroutine driver.
 *
 * The four core collections are implemented as one collection each.
 * Separating data per-domain to different collections may result better for
 * scalability and performance. However, for simplicity, data is unified
 * to reduce domain modelling decision and to keep implementation faster to write.
 */
class MongoDataStore(db: MongoDatabase, collectionName: MongoCollectionName) : DataStore {
    private val accounts = db.getCollection<PlayerAccount>(collectionName.playerAccount)
    private val playerObjects = db.getCollection<PlayerObjects>(collectionName.playerObjects)
    private val playerServerObjects = db.getCollection<PlayerServerObjects>(collectionName.playerServerObjects)
    private val serverObjects = db.getCollection<ServerObjects>(collectionName.serverObjects)

    private val initJob = CoroutineScope(Dispatchers.IO).async { setupCollections() }

    override suspend fun awaitInit() {
        Fancam.info(Tags.Datastore) { "Waiting for MongoDB initialization..." }
        val elapsed = measureTime {
            initJob.await()
        }
        Fancam.info(Tags.Datastore) { "MongoDB initialized in ${elapsed}ms" }
    }

    private suspend fun setupCollections() {
        try {
            val count = accounts.estimatedDocumentCount()
            Fancam.info(Tags.Datastore) { "MongoDB contains $count accounts" }
            prepareServerObjects()
            setupIndexes()
        } catch (e: Exception) {
            Fancam.error(e, Tags.Datastore) { "MongoDB scandal during initialization" }
        }
    }

    private suspend fun setupIndexes() {
        serverObjects.createIndex(Indexes.text())
        Fancam.info(Tags.Datastore) { "Mongo index set up" }
    }

    private suspend fun prepareServerObjects() {
        when (val count = serverObjects.estimatedDocumentCount()) {
            0L -> {
                serverObjects.insertOne(ServerObjects())
            }

            1L -> return

            else -> {
                Fancam.warn(Tags.Datastore) { "Detected multiple server object document count=$count" }
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
        return serverObjects.find(ServerObjectsFilter).firstOrNull()
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
                Fancam.error(tag = Tags.Datastore) {
                    "MongoDB creation not acknowledged: playerId=${account.playerId}, accountAck=$accountAck, pObjAck=$pObjAck, psObjAck=$psObjAck"
                }
                Result.failure(
                    IllegalStateException("MongoDB insert not acknowledged")
                )
            }
        } catch (e: Exception) {
            Fancam.error(e, Tags.Datastore) { "MongoDB creation failed: playerId=${account.playerId}" }
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
                Fancam.error(tag = Tags.Datastore) { "MongoDB deletion not acknowledged: playerId=$playerId, accountAck=$accountAck, pObjAck=$pObjAck, psObjAck=$psObjAck" }
                Result.failure(IllegalStateException("MongoDB deletion not acknowledged"))
            }
        } catch (e: Exception) {
            Fancam.error(e, Tags.Datastore) { "MongoDB deletion failed: playerId=$playerId" }
            Result.failure(e)
        }
    }

    override suspend fun shutdown() = Unit
}
