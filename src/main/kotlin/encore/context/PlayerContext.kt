package encore.context

import encore.datastore.collection.PlayerAccount
import encore.datastore.collection.PlayerId
import encore.network.transport.Connection
import encore.subunit.Subunit
import encore.subunit.scope.PlayerScope

/**
 * Represents the **server-side context** of a connected player.
 *
 * This context holds all data and references required to manage a player session:
 * - [playerId] as the player's unique identifier.
 * - The player's [Connection], used to send and receive network messages.
 * - The player's [PlayerAccount], which includes profile and server-related metadata.
 * - The player's game-specific state, accessible through various [PlayerSubunits].
 *
 * During gameplay, [PlayerContext] is frequently accessed.
 * Typically, right after a player successfully authenticates context instance is created.
 * Context creation is handled by [ContextFactory] and registered in [ContextRegistry].
 */
data class PlayerContext(
    val playerId: PlayerId,
    val connection: Connection,
    val account: PlayerAccount,
    val subunits: PlayerSubunits
)

/**
 * Container for all player-scoped [Subunit] instances.
 *
 * Player subunits encapsulate domain logic that operates at the individual
 * player's level. It typically manages player's data over persistence (database) layer.
 *
 * Player subunits are typically bound to [PlayerScope].
 *
 * Example:
 * - An inventory represents the player's inventory data.
 *   An `InventorySubunit` may expose operations to query or update inventory.
 */
data class PlayerSubunits(
    val example: String = "REPLACE"
) {
    /**
     * Return all player subunit instances.
     */
    fun all(): Set<Subunit<PlayerScope>> {
        return setOf()
    }
}
