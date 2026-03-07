package data

import com.mongodb.kotlin.client.coroutine.MongoCollection
import data.collection.PlayerAccount
import data.collection.PlayerData
import data.collection.ServerData

/**
 * Represent database for this game server.
 *
 * By default, the server always has three collections:
 * - [PlayerAccount]: account of players.
 * - [PlayerData]   : game data of players.
 * - [ServerData]   : server-wide data.
 */
interface Database {
    suspend fun loadPlayerAccount(playerId: String): PlayerAccount?
    suspend fun loadPlayerData(playerId: String): PlayerData?
    suspend fun loadServerData(): ServerData

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
