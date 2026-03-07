package server.core

import io.ktor.util.date.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps track the status of online players.
 */
class OnlinePlayerRegistry {
    private val players = ConcurrentHashMap<String, PlayerStatus>()

    /**
     * Mark a player of [playerId] as online. Does nothing if the player is already online.
     */
    fun markOnline(playerId: String) {
        val now = getTimeMillis()
        players[playerId] = PlayerStatus(
            playerId = playerId,
            onlineSince = now,
            lastNetworkActivity = now,
        )
    }

    /**
     * Mark a player of [playerId] as offline. Does nothing if the player is already offline.
     */
    fun markOffline(playerId: String) {
        players.remove(playerId)
    }

    /**
     * Update the last network activity of [playerId]. Does nothing if the player is not online.
     */
    fun updateLastActivity(playerId: String) {
        players.computeIfPresent(playerId) { _, status ->
            status.copy(lastNetworkActivity = getTimeMillis())
        }
    }

    /**
     * Shutdown the registry by clearing the tracked players.
     */
    fun shutdown() {
        players.clear()
    }
}

/**
 * Represents the current online status of a player.
 *
 * @property onlineSince The timestamp (in milliseconds since epoch)
 *                       when the player came online.
 * @property lastNetworkActivity The timestamp (in milliseconds since epoch)
 *                               of the player's most recent network activity.
 */
data class PlayerStatus(
    val playerId: String,
    val onlineSince: Long,
    val lastNetworkActivity: Long,
)
