package encore.time

import encore.fancam.Fancam
import encore.fancam.Tags
import encore.time.source.SystemTimeSource

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
 * By default, [TimeCenter] set [system] and [game] with a [Timekeeper]
 * supplied with [SystemTimeSource]. This avoids the annoying must init on usage.
 * The [update] can be used to modify the internal timekeeper.
 */
object TimeCenter {
    private var initialized = false
    private var _system: Timekeeper = Timekeeper(SystemTimeSource())
    private var _game: Timekeeper = Timekeeper(SystemTimeSource())

    /**
     * The [Timekeeper] used for system components (e.g., authentication, DB, system timer, etc.).
     */
    val system: Timekeeper = _system

    /**
     * The [Timekeeper] used for game components (e.g., game timer, ticks, daily updates, etc.).
     */
    val game: Timekeeper = _game

    /**
     * Initialize [TimeCenter] with [Timekeeper] instance for system and game.
     */
    fun update(system: Timekeeper, game: Timekeeper) {
        if (initialized) {
            Fancam.warn(Tags.Time) { "TimeCenter was already initialized, ignoring." }
            return
        }
        this._system = system
        this._game = game
        initialized = true
    }
}
