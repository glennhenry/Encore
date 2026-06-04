package game

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import encore.context.ContextFactory
import encore.context.PlayerContext
import encore.context.PlayerSubunits
import encore.datastore.DataStore
import encore.datastore.MongoCollectionName
import encore.datastore.collection.PlayerId
import encore.network.transport.Connection
import encore.subunit.scope.PlayerScope

/**
 * Real implementation of [ContextFactory].
 *
 * Context creation here is user-owned and must be updated accordingly.
 *
 * @property dataStore [DataStore] instance to retrieve player's data.
 */
class RealContextFactory(
    private val dataStore: DataStore,
    private val collectionName: MongoCollectionName,
    private val mongoDatabase: MongoDatabase
) : ContextFactory {
    override suspend fun createContext(
        playerId: PlayerId,
        connection: Connection
    ): PlayerContext {
        val account = requireNotNull(dataStore.getPlayerAccount(playerId)) {
            "Account not exist on context creation for $playerId"
        }

        val subunits = PlayerSubunits(example = "REPLACE")
        val scope = PlayerScope(playerId)
        subunits.debut(scope)

        return PlayerContext(
            playerId = playerId,
            connection = connection,
            account = account,
            subunits = subunits
        )
    }
}
