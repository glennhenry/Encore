package server.messaging.socket

/**
 * Represents a high-level network message ready for processing by message handlers.
 *
 * Implementations of [SocketMessage] encapsulate decoded message data and
 * expose any helper methods or domain-specific behavior required by handlers.
 *
 * This interface defines the boundary between low-level protocol decoding
 * and application-level message handling.
 *
 * Implementations may be:
 * - Generic message wrappers (loosely typed), or
 * - Strongly typed domain messages, often organized as sealed hierarchies.
 *
 * Examples:
 * - A comma-delimited protocol may use a `CommaMessage` implementation that
 *   provides cursor-based accessors such as `nextValue()`.
 * - A JSON-based protocol may define a generic `JsonMessage` as a base type,
 *   with strongly typed subclasses like `LoginRequest`, `SearchQuery`, etc.
 */
interface SocketMessage {
    /**
     * Returns the logical type or identifier of this message,
     * used for handler dispatch.
     */
    fun type(): String

    /**
     * Returns a human-readable representation of this message
     * for debugging and logging.
     */
    override fun toString(): String
}
