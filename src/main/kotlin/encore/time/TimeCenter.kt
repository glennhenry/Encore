package encore.time

import encore.fancam.Fancam

/**
 * Central application component that provides access to [system] and [game]
 * version of [Timekeeper].
 *
 * `TimeCenter` is a registry for two separate time model.
 * Each may keep track a different [Timekeeper] instance, enabling each
 * world to have different rules.
 *
 * For instance, utilizing a real system time for system components, while
 * leveraging a sped up timing model for game mechanics.
 *
 * Should call [initialize] before usage.
 */
object TimeCenter {
    private var _system: Timekeeper? = null

    /**
     * The [Timekeeper] used for system components (e.g., authentication, DB, system timer, etc.).
     */
    val system: Timekeeper
        get() = _system ?: error("System timekeeper is not initialized. Call TimeCenter.initialize() first.")

    private var _game: Timekeeper? = null

    /**
     * The [Timekeeper] used for game components (e.g., game timer, ticks, daily updates, etc.).
     */
    val game: Timekeeper
        get() = _game ?: error("Game timekeeper is not initialized. Call TimeCenter.initialize() first.")

    /**
     * Initialize [TimeCenter] with [Timekeeper] instance for system and game.
     */
    fun initialize(system: Timekeeper, game: Timekeeper) {
        if (_system != null) {
            Fancam.warn { "TimeCenter.initialize() called after initialization. Ignoring." }
            return
        }
        this._system = system
        this._game = game
    }
}
