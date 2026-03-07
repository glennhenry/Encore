package server.messaging.socket

import server.handler.SocketMessageHandler
import server.handler.impl.DefaultHandler
import utils.logging.Logger

/**
 * Central registry and dispatcher for socket message handlers.
 *
 * Responsibilities:
 * - Register handlers via [register].
 * - Resolve and return handlers responsible for a given [SocketMessage].
 *
 * **Contract**: For a given message type, all registered handlers
 * must agree on the same expected concrete [SocketMessage] class.
 * Violations fail fast at registration time.
 */
class SocketMessageDispatcher {
    private val handlers = mutableListOf<SocketMessageHandler<*>>()
    private val handlersByType = mutableMapOf<String, MutableList<SocketMessageHandler<*>>>()
    private val defaultHandler = DefaultHandler()

    /**
     * Register a [SocketMessageHandler].
     *
     * Registration enforces that all handlers bound to the same [SocketMessageHandler.messageType]
     * expect the same concrete [SocketMessage] class. If a mismatch is detected,
     * registration fails immediately to avoid unsafe runtime dispatch.
     *
     * @throws IllegalArgumentException if message type is already bound
     *         to a different expected message class.
     */
    fun <T : SocketMessage> register(handler: SocketMessageHandler<T>) {
        val sameTypes = handlersByType[handler.messageType]

        if (!sameTypes.isNullOrEmpty()) {
            val existing = sameTypes.first().expectedMessageClass
            require(existing == handler.expectedMessageClass) {
                "Handler registration error: messageType='${handler.messageType}' " +
                        "is already bound to ${existing.simpleName}, " +
                        "but handler '${handler.name}' expects ${handler.expectedMessageClass.simpleName}"
            }
        }

        handlers += handler
        handlersByType
            .getOrPut(handler.messageType) { mutableListOf() }
            .add(handler)
    }

    /**
     * Resolve handlers responsible for processing the given [SocketMessage].
     *
     * Dispatch strategy:
     * - Primary: Fast matching by [SocketMessageHandler.messageType].
     * - Secondary: invoke [SocketMessageHandler.shouldHandleUnsafe]
     *   for each handler with same message type.
     * - Fallback: use [DefaultHandler] when no handler matches.
     *
     * @return A non-empty list of handlers. If no handler matches,
     *         a list containing only [DefaultHandler] is returned.
     */
    fun findHandlerFor(msg: SocketMessage): List<SocketMessageHandler<*>> {
        val selected = handlersByType[msg.type()]
            .orEmpty()
            .filter { handler -> handler.shouldHandleUnsafe(msg) }
            .ifEmpty { listOf(defaultHandler) }

        logDispatchment(msg, selected)

        return selected
    }

    private fun logDispatchment(msg: SocketMessage, selected: List<SocketMessageHandler<*>>) {
        Logger.debug {
            buildString {
                appendLine("[SOCKET DISPATCH]")
                appendLine("${Logger.LOG_INDENT_PREFIX} msg (str) : $msg")
                append("${Logger.LOG_INDENT_PREFIX} handlers  : ${selected.joinToString { it.name }}")
            }
        }
    }
}
