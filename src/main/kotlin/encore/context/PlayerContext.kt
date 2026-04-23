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
 * A [PlayerContext] must be created before usage, typically right after a player
 * successfully authenticates. Context creation is handled by [ContextTracker].
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
)
