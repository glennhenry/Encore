package context

import data.Database
import server.core.network.Connection
import java.util.concurrent.ConcurrentHashMap

/**
 * Fake implementation of context tracker for testing purposes.
 *
 * This tracker does not implement method of [ContextTracker]. It instead use
 * [fakeContext] to easily register context for a player.
 */
class FakeContextTracker : ContextTracker {
    val players = ConcurrentHashMap<String, PlayerContext>()

    override suspend fun createContext(playerId: String, connection: Connection, db: Database) = TODO("SHOULD NOT BE USED")

    fun fakeContext(ctx: PlayerContext) {
        players[ctx.playerId] = ctx
    }

    override fun getContext(playerId: String): PlayerContext? {
        return players.get(playerId)
    }

    override fun removeContext(playerId: String) {
        players.remove(playerId)
    }

    override suspend fun shutdown() = Unit
}
