package encore.datastore

import encore.datastore.collection.PlayerAccount
import encore.datastore.collection.PlayerId
import encore.datastore.collection.PlayerObjects
import encore.datastore.collection.PlayerServerObjects
import encore.datastore.collection.ServerObjects

/**
 * Blank (no-operation) implementation for [DataStore].
 */
class BlankDataStore : DataStore {
    override suspend fun awaitInit() = Unit
    override suspend fun playerExists(playerId: PlayerId): Boolean = TODO("NO OPERATION")
    override suspend fun getPlayerAccount(playerId: PlayerId): PlayerAccount = TODO("NO OPERATION")
    override suspend fun getPlayerObjects(playerId: PlayerId): PlayerObjects = TODO("NO OPERATION")
    override suspend fun getPlayerServerObjects(playerId: PlayerId): PlayerServerObjects = TODO("NO OPERATION")
    override suspend fun getServerObjects(): ServerObjects = TODO("NO OPERATION")
    override suspend fun create(account: PlayerAccount, playerObjects: PlayerObjects, playerServerObjects: PlayerServerObjects): Result<Unit> = TODO("NO OPERATION")
    override suspend fun delete(playerId: PlayerId): Result<Unit> = TODO("NO OPERATION")
    override suspend fun shutdown() = TODO("NO OPERATION")
}
