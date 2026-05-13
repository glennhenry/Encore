package encore.network.fanchant

import encore.network.handler.FanchantHandler

/**
 * `Fanchant` represents a high-level network message sent by a client
 * and processed by a [FanchantHandler].
 *
 * Implementations of `Fanchant` encapsulate decoded packet data and may
 * provide helper methods or domain-specific behavior required during
 * message handling.
 *
 * This interface serves as the boundary between low-level protocol decoding
 * and application-level message processing.
 *
 * Implementations may be:
 * - Generic message wrappers (loosely typed), or
 * - Strongly typed domain message, often organized as sealed hierarchies.
 *
 * Examples:
 * - A comma-delimited protocol may use a `CommaMessage` implementation that
 *   provides cursor-based accessors such as `nextValue()`.
 * - A JSON-based protocol may define a generic `JsonMessage` as a base type,
 *   with strongly typed subclasses like `LoginRequest`, `SearchQuery`, etc.
 */
interface Fanchant {
    /**
     * Typed association of this fanchant which is used for handler dispatch.
     */
    val type: FanchantType<*>

    /**
     * Returns a human-readable representation of this packet
     * for debugging and logging.
     */
    override fun toString(): String
}
