package encore.datastore

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
 * @property name Human-readable identifier for logging and debugging.
 */
interface Repository {
    val name: String
}
