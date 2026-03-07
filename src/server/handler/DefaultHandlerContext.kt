package server.handler

import server.core.network.Connection
import server.messaging.socket.SocketMessage

/**
 * Default handler context where send implementation is based on [Connection] object.
 */
class DefaultHandlerContext<T : SocketMessage>(
    private val connection: Connection,
    override var playerId: String,
    override val message: T
) : HandlerContext<T> {
    override suspend fun sendRaw(raw: ByteArray, logOutput: Boolean, logFull: Boolean) {
        connection.write(raw, logOutput, logFull)
    }

    override fun updatePlayerId(playerId: String) {
        connection.updatePlayerId(playerId)
        this.playerId = playerId
    }
}
