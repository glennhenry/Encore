package ws

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents a WebSocket message exchanged between client and server.
 *
 * Each message is encoded as JSON and contains a `type` field and an optional `payload`.
 *
 * The `type` field typically follows the format `"<category>:<action>"`, where:
 * - `category` groups messages by functional domain.
 * - `action` identifies the specific message type within that category.
 *
 * Example:
 *  - A message with type `dev:cmdin` indicates a devtools command input sent from the client.
 *  - A message with type `webchat:in` may indicate a message sent by client from a web-based chat system.
 */
@Serializable
data class WsMessage(
    val type: String,
    val payload: JsonElement? = null,
)

object WsMessageType {
    const val CMD_INPUT = "dev:cmdin"
    const val CMD_OUTPUT = "dev:cmdout"
    const val ERROR = "error"
}
