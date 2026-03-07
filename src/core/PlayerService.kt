package core

/**
 * Represents a player-scoped game services.
 *
 * A service class manages data and domain logic related to a specific game domain for a particular player.
 * It is not responsible for low-level database operations,
 * nor should it handle player identification on each operations.
 * Instead, it stores domain data and provides abstraction to callers.
 *
 * Typically, the service initializes local data through the [init] method.
 * It receives a repository specific to the domain to delegates the low-level database work.
 * Each repository is preferred to be wrapped in try-catch and always return a `Result<T>` type.
 * This is to ensure consistency on error handling across repository.
 *
 * Repository may define CRUD operations only, letting the service define the more complex operations.
 */
interface PlayerService {
    /**
     * Initializes the service for the specified [playerId].
     *
     * This method should be used to load or prepare all data related to the player
     * in this service's domain.
     *
     * @return An empty result just for denoting success or failure.
     */
    suspend fun init(playerId: String): Result<Unit>

    /**
     * Closes the service for the specified [playerId].
     *
     * This method is called when the player logs off or disconnects.
     * It should synchronize any in-memory state with persistent storage
     * to ensure no progress or transient data is lost.
     *
     * @return An empty result just for denoting success or failure.
     */
    suspend fun close(playerId: String): Result<Unit>
}
