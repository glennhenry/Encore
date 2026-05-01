package encore.datastore

import encore.datastore.collection.PlayerAccount
import encore.datastore.collection.PlayerId
import encore.datastore.collection.PlayerObjects
import encore.datastore.collection.PlayerServerObjects
import encore.datastore.collection.ServerObjects

/**
 * A suspendable persistence component that provides access to player and server data.
 *
 * Implementation exposes way to retrieve the core collections and player creation.
 *
 * Higher-level operations such as player creation or alteration of certain player objects
 * fields should be handled by subunits.
 */
interface DataStore {
    /**
     * Ensures the data store is fully initialized.
     *
     * This suspend function will wait until any asynchronous setup is complete.
     * Call this before performing operations that require the store to be ready.
     */
    suspend fun awaitInit()

    /**
     * Returns whether an account associated with [playerId] exists.
     */
    suspend fun playerExists(playerId: PlayerId): Boolean

    /**
     * Returns the [PlayerAccount] for the given [playerId].
     */
    suspend fun getPlayerAccount(playerId: PlayerId): PlayerAccount?

    /**
     * Returns the [PlayerObjects] (game data) for the given [playerId].
     */
    suspend fun getPlayerObjects(playerId: PlayerId): PlayerObjects?

    /**
     * Returns the [PlayerServerObjects] (server-managed player data)
     * for the given [playerId].
     */
    suspend fun getPlayerServerObjects(playerId: PlayerId): PlayerServerObjects?

    /**
     * Returns the [ServerObjects] (global server data).
     */
    suspend fun getServerObjects(): ServerObjects?

    /**
     * Creates a new player with the given account and objects.
     *
     * @return [Result] type denoting success or failure.
     */
    suspend fun create(
        account: PlayerAccount,
        playerObjects: PlayerObjects,
        playerServerObjects: PlayerServerObjects
    ): Result<Unit>

    /**
     * Deletes a player associated with the [playerId].
     */
    suspend fun delete(playerId: PlayerId): Result<Unit>

    /**
     * Shutdown the data store.
     *
     * This should contains the necessary clean-up code before closing.
     */
    suspend fun shutdown()
}
