package encore.repository

/**
 * Represents a data access abstraction over the underlying database.
 *
 * A repository is typically scoped to a specific domain. All repositories
 * must implement this interface, either directly or through
 * a domain-specific interface.
 *
 * Example:
 * `MongoInventoryRepository` > `InventoryRepository` > `Repository`
 *
 * Repositories are not accessed directly, but through [RepositoryRegistry].
 *
 * ### Implementation
 *
 * - Repository should remain focused on a single domain concern.
 * - Repository should wrap the outcome of each operation with a [Result] type.
 * Example:
 * ```
 * interface ItemRepository: Repository {
 *     fun getItem(itemId: String): Result<Item>
 *     fun updateItem(itemId: String, newItem: Item): Result<Unit>
 * }
 * ```
 * - Repository shouldn't log errors and only throw low-level DB
 *   exception (passed via `Result`). Domain specific exception or logging
 *   should be done by the higher component (e.g., subunits).
 *
 * @property name Human-readable identifier for logging and debugging.
 */
interface Repository {
    val name: String
}
