package encore.network.fanchant

import encore.network.handler.FanchantHandler
import encore.network.handler.CatchAllHandler
import encore.fancam.Fancam
import encore.fancam.INDENT
import encore.utils.support.className

/**
 * Component responsible for dispatching [Fanchant] to the appropriate
 * registered [FanchantHandler].
 *
 * Usage:
 * - Register handlers via [register].
 * - Resolve a handler that can handle a given [Fanchant] via [findHandler].
 *
 * **Note**:
 * - Only allow one handler per one fanchant type, violations fail fast
 *   at registration time.
 * - A mismatch between handler's expected fanchant class with the
 *   concrete, runtime materialized fanchant class will not be dispatched
 *   and warned instead.
 */
class FanchantCoordinator {
    private val handlers = mutableMapOf<FanchantType<*>, FanchantHandler<*>>()
    private val catchAllHandler = CatchAllHandler()

    /**
     * Register a [FanchantHandler].
     *
     * Associates the given handler with the fanchant type specified by
     * [FanchantHandler.fanchantType].
     *
     * @throws IllegalArgumentException if a handler for the same fanchant type
     *                                  has already been registered.
     */
    fun <T : Fanchant> register(handler: FanchantHandler<T>) {
        val type = handler.fanchantType
        val existing = handlers[type]

        if (existing != null) {
            throw IllegalArgumentException(
                "Fanchant type '${type.id}' is already associated with ${existing.className()}."
            )
        }

        handlers[type] = handler
    }

    /**
     * Returns the handler associated with the given [Fanchant].
     *
     * Performs a lookup against the registered handlers.
     * If no matching handler is found, a [CatchAllHandler] is returned instead.
     *
     * @return The matching [FanchantHandler], or a [CatchAllHandler]
     *         when no handler is registered for [Fanchant.type].
     */
    fun findHandler(fanchant: Fanchant): FanchantHandler<*> {
        val handler = handlers[fanchant.type]

        Fancam.debug {
            buildString {
                if (handler == null) {
                    appendLine("[SOCKET DISPATCH] -> fallback")
                } else {
                    appendLine("[SOCKET DISPATCH]")
                }
                appendLine("$INDENT fanchant (str): $fanchant")
                append("$INDENT handlers      : ${handler?.className() ?: catchAllHandler.className()}")
            }
        }

        return handler ?: catchAllHandler
    }
}
