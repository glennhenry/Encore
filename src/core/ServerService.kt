package core

import data.collection.ServerData

/**
 * Defines a global service that manages server-wide game state and logic.
 *
 * A [ServerService] is responsible for data and domain logic that apply
 * to the entire server, rather than a single player. It serves a similar role
 * to [PlayerService], but operates at the global scope.
 *
 * Typical implementations depend on a repository that provides access
 * to the [ServerData] collection.
 */
interface ServerService {
    suspend fun init(): Result<Unit>
    suspend fun close(): Result<Unit>
}
