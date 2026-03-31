package encore.db

import com.mongodb.kotlin.client.coroutine.MongoCollection
import encore.db.collection.PlayerAccount
import encore.db.collection.PlayerObjects
import encore.db.collection.ServerObjects

/**
 * Represent database for this game server.
 *
 * By default, the server always has three collections:
 * - [PlayerAccount] : account of players.
 * - [PlayerObjects] : game data of players.
 * - [ServerObjects] : server-wide data.
 */
interface Database {
    suspend fun loadPlayerAccount(playerId: String): PlayerAccount?
    suspend fun loadPlayerObjects(playerId: String): PlayerObjects?
    suspend fun loadServerObjects(): ServerObjects

    /**
     * Get a particular *mongo* collection without type safety.
     *
     * This is typically used to inject repository with collection data.
     */
    fun <T : Any> getCollection(name: String): MongoCollection<T>

    /**
     * Create a player with the provided username and password.
     *
     * Implementor should populate all three collections for the new player.
     * May throw error if insert is failed.
     *
     * @return playerId (UUID) of the newly created player.
     */
    suspend fun createPlayer(username: String, password: String): String

    suspend fun shutdown()
}
