package encore.subunit

import encore.subunit.scope.SubunitScope
import encore.subunit.scope.ServerScope

/**
 * A `Subunit` is a scope-bound service responsible for managing
 * domain data and behavior within a specific [SubunitScope].
 *
 * It acts as a domain-level abstraction that may:
 * - Encapsulate business logic for a specific domain.
 * - Maintain an in-memory representation of scoped data
 *   to reduce repeated database access.
 * - Coordinate persistence through repositories when needed.
 *
 * A `Subunit` may also function as a "stateless subsystem",
 * providing domain-specific functionality without necessarily
 * managing data or persistence.
 *
 * For example, a Subunit bound to [ServerScope] operates at the
 * server level, supporting a particular server concern.
 *
 * Subunits typically are not aware of each other and are orchestrated
 * externally (e.g., by handlers or controllers).
 *
 * Lifecycle:
 * - [debut] initializes the subunit.
 * - [disband] closes the subunit.
 *
 * Guideline:
 * - It should remain focused on a single domain concern.
 * - It should not perform low-level database operations directly.
 * - It should provide abstraction to caller from the repository.
 */
interface Subunit<T : SubunitScope> {
    /**
     * Debut the subunit for the given [scope].
     *
     * This method should be used to prepare or load the required state.
     *
     * @return A result type to denote success or failure.
     */
    suspend fun debut(scope: T): Result<Unit>

    /**
     * Disband the subunit for the given [scope].
     *
     * This method is called when the subunit closes (e.g., server shutdown,
     * player logs off or disconnects).
     *
     * It should do the necessary clean-up or persist any in-memory state
     * to storage to ensure no progress or transient data is lost.
     *
     * @return A result type to denote success or failure.
     */
    suspend fun disband(scope: T): Result<Unit>
}
