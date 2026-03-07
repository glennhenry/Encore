package context

import com.mongodb.kotlin.client.coroutine.MongoCollection
import data.Database
import data.collection.PlayerData
import server.core.network.Connection
import java.util.concurrent.ConcurrentHashMap

/**
 * Default implementation of [ContextTracker] which is based on real player
 * [Connection] and [Database].
 */
class DefaultContextTracker: ContextTracker {
    private val players = ConcurrentHashMap<String, PlayerContext>()

    /**
     * Creates and registers a new [PlayerContext] for the given player.
     *
     * This function loads the player's account from the [Database], initializes
     * the associated [PlayerServices], and stores the resulting context in [players].
     *
     * @param playerId The unique identifier of the player.
     * @param connection The player's active network [Connection].
     * @param db The [Database] instance used to load account data and initialize services.
     *
     * @throws IllegalArgumentException If the player's account data cannot be found.
     */
    override suspend fun createContext(
        playerId: String,
        connection: Connection,
        db: Database
    ) {
        val playerAccount =
            requireNotNull(db.loadPlayerAccount(playerId)) { "Missing PlayerAccount for playerId=$playerId" }

        val context = PlayerContext(
            playerId = playerId,
            connection = connection,
            account = playerAccount,
            services = initializeServices(playerId, db)
        )
        players[playerId] = context
    }

    private suspend fun initializeServices(
        playerId: String,
        db: Database,
    ): PlayerServices {
        val playerDataCollection = db.getCollection<MongoCollection<PlayerData>>("player_data")

        // REPLACE add

        return PlayerServices(
            example = ""
        )
    }

    override fun getContext(playerId: String): PlayerContext? {
        return players[playerId]
    }

    override fun removeContext(playerId: String) {
        players.remove(playerId)
    }

    override suspend fun shutdown() {
        players.values.forEach {
            it.connection.shutdown()
        }
        players.clear()
    }
}
