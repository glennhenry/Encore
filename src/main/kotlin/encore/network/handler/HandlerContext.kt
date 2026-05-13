package encore.network.handler

import encore.datastore.collection.PlayerId
import encore.network.transport.Connection
import encore.network.fanchant.Fanchant

/**
 * Encapsulate objects and data needed by handlers to handle network messages.
 *
 * @param T Concrete type of the message.
 */
interface HandlerContext<out T : Fanchant> {
    /**
     * The player in-game unique identifier.
     */
    var playerId: PlayerId

    /**
     * The received network message to be handled.
     */
    val fanchant: T

    /**
     * Send the client [raw] bytes.
     *
     * This function merely send bytes to the connection.
     * Serialization is caller responsibility, and can be done
     * by calling the appropriate serializer utility.
     */
    suspend fun sendRaw(raw: ByteArray, logOutput: Boolean = true, logFull: Boolean = false)

    /**
     * To update the playerId for this connection (usually goes through [Connection.updatePlayerId]).
     */
    fun updatePlayerId(playerId: PlayerId)
}
