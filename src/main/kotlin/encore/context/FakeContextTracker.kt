package encore.context

import encore.datastore.DataStore
import encore.datastore.collection.PlayerId
import encore.network.transport.Connection
import java.util.concurrent.ConcurrentHashMap

/**
 * Fake implementation of context tracker for testing purposes.
 *
 * This tracker does not implement method of [ContextTracker]. It instead use
 * [fakeContext] to easily register context for a player.
 */
class FakeContextTracker : ContextTracker {
    val players = ConcurrentHashMap<String, PlayerContext>()

    override suspend fun createContext(playerId: PlayerId, connection: Connection, db: DataStore) = TODO("SHOULD NOT BE USED")

    fun fakeContext(ctx: PlayerContext) {
        players[ctx.playerId] = ctx
    }

    override fun getContext(playerId: PlayerId): PlayerContext? {
        return players.get(playerId)
    }

    override fun removeContext(playerId: PlayerId) {
        players.remove(playerId)
    }

    override suspend fun shutdown() = Unit
}
