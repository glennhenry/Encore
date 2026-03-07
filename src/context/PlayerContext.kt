package context

import data.collection.PlayerAccount
import server.core.network.Connection
import core.PlayerService

/**
 * Represents the **server-side context** of a connected player.
 *
 * This context holds all data and references required to manage a player session:
 * - The player's [Connection], used to send and receive network messages.
 * - The player's [PlayerAccount], which includes profile and server-related metadata.
 * - The player's game-specific state, accessible through various [PlayerServices].
 *
 * A [PlayerContext] must be created before use—typically right after a player
 * successfully authenticates. Context creation is handled by [ContextTracker].
 */
data class PlayerContext(
    val playerId: String,
    val connection: Connection,
    val account: PlayerAccount,
    val services: PlayerServices
)

/**
 * A container that holds all **service instances** related to a specific player.
 *
 * Each [PlayerService] encapsulates domain logic and manages the player's in-memory state,
 * while also providing an abstraction layer over persistence (database) operations.
 * Controllers or message handlers interact with the player indirectly through these services.
 *
 * All service instances should be initialized before use, usually during
 * [PlayerContext] creation.
 */
data class PlayerServices(
    val example: String = "REPLACE"
)
