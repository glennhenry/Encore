package encoreTest.utils

import encore.fancam.Fancam
import encore.fancam.impl.RehearsalFancam

/**
 * Global test helper to create and access [RehearsalFancam], ensuring the same
 * object is used in the initialized fancam implemention of [Fancam] used by
 * various system components.
 *
 * Example:
 * ```
 * // create and initialize facade
 * // must call in @BeforeTest to ensure system components get the same object
 * @BeforeTest
 * fun setup() {
 *     TestFancam.create()
 * }
 *
 * // get and use
 * TestFancam.get().assertLogHas(...)
 *
 * // clear all logs
 * TestFancam.get().clear()
 * ```
 */
object TestFancam {
    private lateinit var fancam: RehearsalFancam

    fun create() {
        fancam = RehearsalFancam()
        Fancam.initialize(fancam)
    }

    fun get() = fancam

    fun clear() {
        fancam.clearAll()
    }
}
