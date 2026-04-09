package encore.repository

import encore.fancam.Fancam
import kotlin.reflect.KClass

/**
 * Registry for server-side [Repository] instances.
 *
 * Repositories aren't accessed directly. Instead, they are registered
 * and accessed through this registry. This ensures a consistent dependency
 * management across the server lifecycle.
 *
 * - Register repositories once via a DSL ([register]).
 * - Provide type-safe lookup via [get].
 *
 * This registry is intended to be initialized during application startup.
 * Initialization would include registration of the repositories which may
 * depends on the application startup itself.
 */
class RepositoryRegistry {
    private val repos = mutableMapOf<KClass<out Repository>, Repository>()

    /**
     * Registers repositories using a DSL-style initializer.
     *
     * This method will ignore subsequent register call if the registry
     * has already been populated previously.
     *
     * Example:
     * ```
     * registry.register {
     *     add(UserRepository::class) { DefaultUserRepository() }
     * }
     * ```
     *
     * @param init DSL block used to populate the registry.
     */
    fun register(init: RepositoryInitContext.() -> Unit) {
        if (repos.isNotEmpty()) {
            Fancam.warn { "RepositoryRegistry.register() called after initialization. Ignored." }
            return
        }
        init(RepositoryInitContext(repos))
        Fancam.trace { "RepositoryRegistry initialized with ${repos.size} instances." }
    }

    /**
     * Returns the registered implementation for the given repository type.
     *
     * @param cls The repository interface or base class.
     * @return The registered instance of type [T].
     *
     * @throws NoSuchElementException if no repository is registered for [cls].
     * @throws IllegalStateException if called before [register].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Repository> get(cls: KClass<T>): T {
        if (repos.isEmpty()) {
            throw IllegalStateException("RepositoryRegistry accessed before initialization.")
        }

        return repos[cls] as? T
            ?: throw NoSuchElementException("No repository registered for ${cls.simpleName}.")
    }

    /**
     * DSL context used to register repositories.
     *
     * This shouldn't be instantiated manually.
     */
    class RepositoryInitContext(
        private val repos: MutableMap<KClass<out Repository>, Repository>
    ) {
        /**
         * Registers a repository implementation.
         *
         * @param cls The repository type used as the lookup key.
         * @param factory Creates the concrete repository instance.
         */
        fun <T : Repository> add(cls: KClass<T>, factory: () -> T) {
            repos[cls] = factory()
        }
    }
}
