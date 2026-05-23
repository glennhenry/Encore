package encore.context

import encore.datastore.collection.PlayerId
import encore.network.transport.Connection
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages [PlayerContext] of all connected players.
 *
 * This class is responsible for:
 * - Creating, registering and storing [PlayerContext].
 * - Provide lookup during gameplay.
 *
 * @property factory [ContextFactory] instance used for creating contexts.
 */
class ContextRegistry(private val factory: ContextFactory) {
    private val players = ConcurrentHashMap<String, PlayerContext>()

    /**
     * Register [PlayerContext] for the given player.
     *
     * Context creation depends on [ContextFactory] from the constructor.
     *
     * @param playerId Unique identifier of the player.
     * @param connection [Connection] object.
     * @return The newly created context.
     */
    suspend fun createContext(playerId: PlayerId, connection: Connection): PlayerContext {
        val context = factory.createContext(playerId, connection)
        players[playerId] = context
        return context
    }

    /**
     * Get [PlayerContext] associated with [playerId], if exists.
     */
    fun getContext(playerId: PlayerId): PlayerContext? {
        return players[playerId]
    }

    /**
     * Remove [PlayerContext] associated with [playerId].
     */
    fun removeContext(playerId: PlayerId) {
        players.remove(playerId)
    }
}
