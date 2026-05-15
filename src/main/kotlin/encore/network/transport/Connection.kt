package encore.network.transport

import encore.datastore.collection.PlayerId
import kotlinx.coroutines.CoroutineScope

/**
 * Represents an active client connection to the socket server.
 *
 * Implementations of this interface define how data is read from and written to
 * the underlying transport (e.g., TCP socket, WebSocket, or virtual connection).
 */
interface Connection {
    /**
     * The coroutine scope for this connection.
     */
    val connectionScope: CoroutineScope

    /**
     * Identity information.
     */
    val identity: ConnectionIdentity

    /**
     * Shorthand for [ConnectionIdentity.playerId]
     */
    val playerId: PlayerId get() = identity.playerId ?: "[Undetermined]"

    /**
     * Shorthand for [ConnectionIdentity.username]
     */
    val username: String get() = identity.username ?: "[Undetermined]"

    /**
     * Shorthand for [ConnectionIdentity.remoteAddress]
     */
    val address: String get() = identity.remoteAddress

    /**
     * Reads data sent by the client.
     *
     * @return A [Pair] where the first element is the number of bytes read
     *         (or `-1` if the connection has ended), and the second element
     *         contains the raw bytes received.
     */
    suspend fun read(): Pair<Int, ByteArray>

    /**
     * Sends data to the client.
     *
     * @param input The raw serialized bytes to transmit.
     * @param logOutput Whether the output should be logged.
     * @param logFull Whether the log message should be full.
     */
    suspend fun write(input: ByteArray, logOutput: Boolean = true, logFull: Boolean = false)

    /**
     * Acknowledge this connection and attach identity information.
     */
    fun acknowledge(playerId: PlayerId, username: String)

    /**
     * Closes the connection and performs clean-up.
     */
    suspend fun shutdown()

    /**
     * Returns a human-readable string representation of this connection
     * for logging and debugging purpose.
     */
    override fun toString(): String
}
