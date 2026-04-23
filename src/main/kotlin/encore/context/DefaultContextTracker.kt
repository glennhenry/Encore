package encore.context

import encore.datastore.DataStore
import encore.datastore.collection.PlayerId
import encore.network.transport.Connection
import java.util.concurrent.ConcurrentHashMap

/**
 * Default implementation of [ContextTracker] which is based on real player
 * [Connection] and [DataStore].
 */
class DefaultContextTracker: ContextTracker {
    private val players = ConcurrentHashMap<String, PlayerContext>()

    /**
     * Creates and registers a new [PlayerContext] for the given player.
     *
     * This function loads the player's account from the [DataStore], initializes
     * the associated [PlayerSubunits], and stores the resulting context in [players].
     *
     * @param playerId The unique identifier of the player.
     * @param connection The player's active network [Connection].
     * @param db The [DataStore] instance used to load account data and initialize subunits.
     *
     * @throws IllegalArgumentException If the player's account data cannot be found.
     */
    override suspend fun createContext(
        playerId: PlayerId,
        connection: Connection,
        db: DataStore
    ) {
        val playerAccount = requireNotNull(db.getPlayerAccount(playerId)) { "Missing PlayerAccount for playerId=$playerId" }

        val context = PlayerContext(
            playerId = playerId,
            connection = connection,
            account = playerAccount,
            subunits = initializeSubunits(playerId, db)
        )
        players[playerId] = context
    }

    private suspend fun initializeSubunits(
        playerId: PlayerId,
        db: DataStore,
    ): PlayerSubunits {
        val playerObjectsCollection = db.getPlayerObjects(playerId)

        // REPLACE add

        return PlayerSubunits(
            example = ""
        )
    }

    override fun getContext(playerId: PlayerId): PlayerContext? {
        return players[playerId]
    }

    override fun removeContext(playerId: PlayerId) {
        players.remove(playerId)
    }

    override suspend fun shutdown() {
        players.values.forEach {
            it.connection.shutdown()
        }
        players.clear()
    }
}
