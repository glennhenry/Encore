package encore.db

import com.mongodb.kotlin.client.coroutine.MongoCollection
import encore.db.collection.PlayerAccount
import encore.db.collection.PlayerObjects
import encore.db.collection.ServerObjects

/**
 * Empty implementation (no operation) of [Database] only used for testing purposes.
 */
class EmptyDatabase : Database {
    override suspend fun loadPlayerAccount(playerId: String): PlayerAccount = TODO("ONLY TEST")
    override suspend fun loadPlayerObjects(playerId: String): PlayerObjects = TODO("ONLY TEST")
    override suspend fun loadServerObjects(): ServerObjects = TODO("ONLY TEST")
    override fun <T : Any> getCollection(name: String): MongoCollection<T> = TODO("ONLY TEST")
    override suspend fun createPlayer(username: String, password: String): String = TODO("ONLY TEST")
    override suspend fun shutdown() = TODO("ONLY TEST")
}
