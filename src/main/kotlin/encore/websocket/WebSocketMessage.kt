package encore.websocket

import encore.fancam.Fancam
import encore.fancam.INDENT
import encore.fancam.Tags
import encore.serialization.JSON
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents a WebSocket message exchanged between client and server.
 *
 * Each message is encoded as JSON and transmitted as texts.
 * A message contains:
 * - `type` field which is a string identifier used to distinguish
 *   the purpose as well as the payload structure of a websocket message.
 * - An optional, non-nullable `payload` to exchange data.
 */
@Serializable
data class WebSocketMessage(
    val type: String,
    val payload: JsonElement,
)

/**
 * WebSocket utility to send a websocket messaage.
 *
 * Response will be formatted in JSON and transmitted in [Frame.Text] form.
 * The repsonse will also be logged.
 *
 * @param clientId The client unique identifier, for logging purpose.
 * @param type The type of this websocket message.
 * @param payload The payload of the message in [JsonElement].
 */
suspend fun DefaultWebSocketServerSession.sendJson(clientId: String, type: String, payload: JsonElement) {
    Fancam.debug(Tags.Websocket) {
        buildString {
            appendLine("##### [WebSocket Send]")
            appendLine("$INDENT clientId   : $clientId")
            appendLine("$INDENT type       : $type")
            append("$INDENT payload    : $payload")
        }
    }

    send(Frame.Text(JSON.encode(WebSocketMessage(type, payload))))
}
