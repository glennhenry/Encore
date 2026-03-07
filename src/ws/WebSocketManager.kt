package ws

import context.ServerContext
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import utils.JSON
import utils.logging.Logger
import java.util.concurrent.ConcurrentHashMap

typealias ClientSessions = ConcurrentHashMap<String, DefaultWebSocketServerSession>

/**
 * Track websocket connections.
 */
class WebSocketManager {
    private lateinit var serverContext: ServerContext
    private val clients = ClientSessions()

    /**
     * Add client's websocket session.
     *
     * Do nothing if the client is already added before.
     */
    fun addClient(clientId: String, session: DefaultWebSocketServerSession) {
        if (!clients.contains(clientId)) {
            clients[clientId] = session
        }
    }

    /**
     * Remove a tracked client session.
     *
     * @return `true` if successfully removed.
     */
    fun removeClient(clientId: String): Boolean {
        return clients.remove(clientId) != null
    }

    fun getAllClients(): ClientSessions {
        return clients
    }

    /**
     * Get websocket session from [clientId].
     */
    fun getSessionFromId(clientId: String): DefaultWebSocketServerSession? {
        return clients[clientId]
    }

    /**
     * Handle websocket message from the client [session].
     */
    suspend fun handleMessage(session: DefaultWebSocketServerSession, message: WsMessage) {
        if (message.payload == null) {
            session.send(
                createMessage(
                    type = "error",
                    payload = buildJsonObject {
                        put("message", "JSON payload is null")
                    }
                )
            )
            return
        }

        when (message.type) {
            WsMessageType.CMD_INPUT -> {
                val rawCmd = JSON.json.decodeFromJsonElement<String>(message.payload)
                val result = serverContext.commandDispatcher.handleRawCommand(rawCmd)

                session.send(
                    createMessage(
                        type = WsMessageType.CMD_OUTPUT,
                        payload = buildJsonObject {
                            put("result", result.toString())
                        }
                    )
                )
            }
        }
    }

    private fun createMessage(type: String, payload: JsonElement): Frame {
        return Frame.Text(JSON.encode(WsMessage(type, payload)))
    }

    fun init(context: ServerContext) {
        this.serverContext = context
    }
}
