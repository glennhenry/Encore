package server.handler

import server.messaging.socket.SocketMessage
import server.messaging.format.MessageFormat
import kotlin.reflect.KClass

/**
 * Template for a socket message handler.
 *
 * A [SocketMessageHandler] processes a specific kind of [SocketMessage] produced
 * by the message decoding and materialization pipeline.
 *
 * Each handler is expected to:
 * - Declare the logical message type it handles via [messageType].
 * - Specify the concrete [SocketMessage] implementation it expects through
 *   the generic parameter [T] and [expectedMessageClass].
 *
 * The [SocketMessage] instance is produced by [MessageFormat.materialize].
 *
 * Examples:
 * - A JSON-based protocol may define a handler with `T = JsonMessage`,
 *   where `JsonMessage` wraps a `Map<String, Any>` and provides helper accessors.
 * - A more specific handler may declare `T = LoginRequest`, where
 *   `LoginRequest` is a strongly typed domain message implementing `SocketMessage`.
 *
 * Handler dispatch behavior:
 * - Incoming [SocketMessage] instances are routed to handlers by a dispatcher.
 * - Dispatch is performed by matching [messageType] against
 *   [SocketMessage.type] (fast pre-filtering).
 * - Then, the [shouldHandleUnsafe] will be called for each matched handlers
 *   which verifies that the message is compatible with the handler’s expected
 *   message class.
 *
 * Handlers may override [shouldHandle] to apply additional filtering logic.
 *
 * **Contract**:
 * - All handlers registered under the same [messageType] must expect
 * the same concrete [SocketMessage] implementation (by architecture).
 * - The dispatcher guarantees that [handle] is only invoked when the runtime
 * message instance is compatible with [T].
 * - The unchecked cast required to bridge from [SocketMessage] to [T] is
 * centralized in [handleUnsafe].
 *
 * The detailed process:
 * ```
 * shouldHandleUnsafe
 *   ↓
 * handleUnsafe
 *   ↓
 * shouldHandle
 *   ↓
 * handle
 * ```
 *
 * @param T The concrete implementation of [SocketMessage] this handler expects.
 */
interface SocketMessageHandler<T : SocketMessage> {
    /**
     * Human-readable name of the handler for logging and debugging.
     */
    val name: String

    /**
     * Logical message type or identifier that this handler is responsible for.
     *
     * This value is compared against [SocketMessage.type] during dispatch.
     *
     * **Important**: all socket message's type should be different, regardless
     * when they are different [SocketMessage] implementation. This is because
     * dispatchment logic rely on type.
     */
    val messageType: String

    /**
     * Concrete message class this handler expects (which same as [T]).
     * Use the syntax `classname::class`.
     */
    val expectedMessageClass: KClass<T>

    /**
     * Dispatcher-facing predicate.
     *
     * Determines whether this handler is eligible to process the given
     * [SocketMessage] instance.
     *
     * The default implementation checks:
     * - [messageType] matches [SocketMessage.type], and
     * - the message is an instance of [expectedMessageClass].
     *
     * **Note**: This method is used by the dispatcher and shouldn't be called
     * directly. It shouldn't be overridden either.
     */
    fun shouldHandleUnsafe(message: SocketMessage): Boolean =
        messageType == message.type() &&
                expectedMessageClass.isInstance(message)

    /**
     * Handler-facing predicate.
     *
     * With [shouldHandleUnsafe] being called first, it is guaranteed
     * that the message has been proven (at runtime) to be compatible with [T].
     *
     * By default, this is implemented to always return `true` since [messageType]
     * and the message class is already compared in [shouldHandleUnsafe].
     * Handlers may override this method to apply additional domain-specific filtering.
     */
    fun shouldHandle(message: T): Boolean = true

    /**
     * Runtime bridge between untyped dispatch and type-safe handler logic.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun handleUnsafe(ctx: HandlerContext<SocketMessage>) {
        val msg = ctx.message
        if (!expectedMessageClass.isInstance(msg)) return

        // important cast to convert the generic SocketMessage into specific T
        val typedCtx = ctx as HandlerContext<T>
        if (!shouldHandle(typedCtx.message)) return

        handle(typedCtx)
    }

    /**
     * Handles an incoming socket message.
     *
     * This method is only invoked after dispatch invariants have been
     * validated by [handleUnsafe].
     *
     * @param ctx The handler context, containing the decoded message,
     * connection metadata (such as player ID), and utilities such as
     * [HandlerContext.sendRaw] for sending responses.
     */
    suspend fun handle(ctx: HandlerContext<T>)
}
