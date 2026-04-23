package encore.context

import encore.datastore.DataStore
import encore.datastore.collection.PlayerId
import encore.network.transport.Connection

/**
 * Represent tracker for [PlayerContext] of all connected players.
 *
 * This class is responsible for:
 * - Creating new [PlayerContext] instances.
 * - Creating a context includes initializing all [PlayerSubunits] for each player.
 * - Storing active player contexts for lookup and management during gameplay.
 */
interface ContextTracker {
    /**
     * Creates and registers a new [PlayerContext] for the given player.
     */
    suspend fun createContext(playerId: PlayerId, connection: Connection, db: DataStore)

    /**
     * Get context of [playerId].
     */
    fun getContext(playerId: PlayerId): PlayerContext?

    /**
     * Remove context of [playerId].
     */
    fun removeContext(playerId: PlayerId)

    /**
     * Shutdown the tracker.
     */
    suspend fun shutdown()
}
