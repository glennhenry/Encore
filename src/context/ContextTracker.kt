package context

import data.Database
import server.core.network.Connection

/**
 * Represent tracker for [PlayerContext] of all connected players.
 *
 * This class is responsible for:
 * - Creating new [PlayerContext] instances.
 * - Creating a context includes initializing all [PlayerServices] for each player.
 * - Storing active player contexts for lookup and management during gameplay.
 */
interface ContextTracker {
    /**
     * Creates and registers a new [PlayerContext] for the given player.
     */
    suspend fun createContext(
        playerId: String,
        connection: Connection,
        db: Database
    )

    /**
     * Get context of [playerId].
     */
    fun getContext(playerId: String): PlayerContext?

    /**
     * Remove context of [playerId].
     */
    fun removeContext(playerId: String)

    /**
     * Shutdown the tracker.
     */
    suspend fun shutdown()
}
