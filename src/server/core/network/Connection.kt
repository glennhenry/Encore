package server.core.network

import kotlinx.coroutines.CoroutineScope

/**
 * Represents an active client (player) connection to the server.
 *
 * Implementations of this interface define how data is read from and written to
 * the underlying transport (e.g., TCP socket, WebSocket, or virtual connection).
 *
 * @property remoteAddress The IP address or network identifier of the client.
 * @property connectionScope The coroutine scope with this connection.
 * @property playerId The in-game identifier of the connected player.
 * @property playerName The display name or username associated with the player.
 */
interface Connection {
    val remoteAddress: String
    val connectionScope: CoroutineScope
    var playerId: String
    var playerName: String

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
     * To update the playerId for this connection.
     */
    fun updatePlayerId(playerId: String)

    /**
     * Closes the connection and releases any associated resources.
     */
    suspend fun shutdown()

    /**
     * Returns a human-readable string representation of this connection.
     */
    override fun toString(): String
}
