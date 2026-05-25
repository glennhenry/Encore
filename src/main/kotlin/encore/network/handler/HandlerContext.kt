package encore.network.handler

import encore.datastore.collection.PlayerId
import encore.network.fanchant.Fanchant
import encore.network.transport.ConnectionIdentity

/**
 * Encapsulate objects and data needed by handlers to handle network messages.
 *
 * @param T Concrete type of the message.
 */
interface HandlerContext<out T : Fanchant> {
    /**
     * The identifier for the player's connection.
     */
    val connectionIdentity: ConnectionIdentity

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

    fun acknowledge(playerId: PlayerId, username: String)
}
