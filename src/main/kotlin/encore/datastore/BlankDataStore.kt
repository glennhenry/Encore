package encore.datastore

import encore.datastore.collection.PlayerAccount
import encore.datastore.collection.PlayerObjects
import encore.datastore.collection.ServerObjects

/**
 * Blank (no-operation) implementation for [DataStore].
 */
class BlankDataStore : DataStore {
    override suspend fun awaitInit() = Unit
    override suspend fun playerExists(playerId: String): Boolean = TODO("NO OPERATION")
    override suspend fun getPlayerAccount(playerId: String): PlayerAccount = TODO("NO OPERATION")
    override suspend fun getPlayerObjects(playerId: String): PlayerObjects = TODO("NO OPERATION")
    override suspend fun getServerObjects(): ServerObjects = TODO("NO OPERATION")
    override suspend fun create(account: PlayerAccount, objects: PlayerObjects): Result<Unit> = TODO("NO OPERATION")
    override suspend fun delete(playerId: String): Result<Unit> = TODO("NO OPERATION")
    override suspend fun shutdown() = TODO("NO OPERATION")
}
