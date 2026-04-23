package encore.activity

import encore.datastore.collection.PlayerId
import encore.subunit.Subunit
import encore.subunit.scope.ServerScope
import io.ktor.util.date.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Server subunit for tracking players activity status.
 *
 * This is used for:
 * - Tracking whether a player is offline/online.
 * - Get the last active time of a player.
 *
 * Typically, player activity is determined from the network activity of the socket server.
 */
class PlayerActivitySubunit : Subunit<ServerScope> {
    private val onlinePlayers = ConcurrentHashMap<String, PlayerActivity>()

    /**
     * Mark the [playerId] as online. Does nothing if the player is already online.
     */
    fun markOnline(playerId: PlayerId) {
        val now = getTimeMillis()
        onlinePlayers[playerId] = PlayerActivity(
            playerId = playerId,
            onlineSince = now,
            lastNetworkActivity = now,
        )
    }

    /**
     * Mark the [playerId] as offline. Does nothing if the player is already offline.
     */
    fun markOffline(playerId: PlayerId) {
        onlinePlayers.remove(playerId)
    }

    /**
     * Returns whether player with [playerId] is currently online.
     */
    fun isOnline(playerId: PlayerId): Boolean {
        return onlinePlayers.contains(playerId)
    }

    /**
     * Returns whether player with [playerId] is currently online.
     */
    fun isOffline(playerId: PlayerId): Boolean {
        return !onlinePlayers.contains(playerId)
    }

    /**
     * Update the last network activity of [playerId]. Does nothing if the player is not online.
     */
    fun updateLastActivity(playerId: PlayerId) {
        onlinePlayers.computeIfPresent(playerId) { _, status ->
            status.copy(lastNetworkActivity = getTimeMillis())
        }
    }

    override suspend fun debut(scope: ServerScope): Result<Unit> = Result.success(Unit)
    override suspend fun disband(scope: ServerScope): Result<Unit> {
        onlinePlayers.clear()
        return Result.success(Unit)
    }
}
