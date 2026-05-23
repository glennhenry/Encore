package encore.context

import encore.datastore.collection.PlayerId
import encore.network.transport.Connection

/**
 * Represent a factory responsible for [PlayerContext] object creation.
 *
 * Implementation decides how to construct a [PlayerContext] from the active
 * player's connection given during the runtime.
 */
fun interface ContextFactory {
    /**
     * Create [PlayerContext] for [playerId].
     *
     * @param playerId Unique identifier of the player.
     * @param connection [Connection] object.
     */
    suspend fun createContext(playerId: PlayerId, connection: Connection): PlayerContext
}
