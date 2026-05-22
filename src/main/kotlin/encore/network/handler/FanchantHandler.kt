package encore.network.handler

import encore.network.fanchant.Fanchant
import encore.network.fanchant.FanchantType

/**
 * Represent a handler for network messages.
 *
 * A [FanchantHandler] processes a specific kind of [Fanchant] produced
 * by the message decoding and materialization pipeline.
 *
 * Each handler is expected to declare the logical fanchant type
 * it handles via [fanchantType].
 *
 * Handler is dispatched by matching between [Fanchant.type] and the [fanchantType]
 * of handler. This mean that there should only be **one handler per one fanchant type**.
 *
 * For instance:
 * - A protocol may associate a `LoginFanchant` with `LoginFanchantType`.
 *   A `LoginFanchantHandler` declaring `T = LoginFanchant` and
 *   `fanchantType = LoginFanchantType` will only receive fanchants
 *   exclusively materialized into `LoginFanchant`.
 *
 * @param T The concrete implementation of [Fanchant] this handler expects.
 */
interface FanchantHandler<T : Fanchant> {
    /**
     * Type association between the protocol identifier in a network message
     * with the concrete [Fanchant] class that this handler is responsible to handle.
     *
     * This value is compared against [Fanchant.type] during dispatchment.
     *
     * **Important**: all fanchant type should be different, regardless
     * if they are a different [Fanchant] implementation. This is because
     * dispatchment logic primarily rely on type.
     */
    val fanchantType: FanchantType<T>

    /**
     * A runtime bridge between untyped dispatch and type-safe handler logic.
     */
    suspend fun handleUnsafe(ctx: HandlerContext<Fanchant>) {
        @Suppress("UNCHECKED_CAST")
        handle(ctx as HandlerContext<T>)
    }

    /**
     * Handles an incoming [Fanchant] with the given handler [ctx].
     *
     * @param ctx Context of message handling. This contains the materialized message,
     *            player's metadata (e.g., player ID), and transport method
     *            [HandlerContext.sendRaw] for sending responses.
     */
    suspend fun handle(ctx: HandlerContext<T>)
}
