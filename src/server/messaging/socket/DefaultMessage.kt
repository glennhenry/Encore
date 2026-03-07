package server.messaging.socket

import server.messaging.format.DefaultFormat

/**
 * Default fallback implementation of [SocketMessage].
 *
 * This message represents raw, uninterpreted data decoded as a string.
 * No protocol-specific structure or semantics are applied.
 *
 * Behavior:
 * - [type] always returns a fixed identifier "DefaultMessage-type".
 * - [toString] returns the decoded data.
 *
 * This message works together with [DefaultFormat] to guarantee
 * the report of unknown or unsupported message format.
 */
class DefaultMessage(val decoded: String) : SocketMessage {
    override fun type(): String = "DefaultMessage-type"
    override fun toString(): String = "DefaultMessage($decoded)"
}
