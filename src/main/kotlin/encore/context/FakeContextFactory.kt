package encore.context

import encore.datastore.collection.PlayerId
import encore.network.transport.Connection

/**
 * Fake implementation of [ContextFactory] where context creation
 * is solely provided from the input map [contexts] in the constructor.
 */
class FakeContextFactory(private val contexts: Map<String, PlayerContext>) : ContextFactory {
    override suspend fun createContext(playerId: PlayerId, connection: Connection): PlayerContext {
        return requireNotNull(contexts[playerId]) { "$playerId not found on input contexts." }
    }
}
