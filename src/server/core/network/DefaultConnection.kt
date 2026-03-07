package server.core.network

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import utils.functions.hexString
import utils.functions.safeAsciiString
import utils.logging.Logger
import utils.logging.Logger.LOG_INDENT_PREFIX

/**
 * Default implementation of [Connection] which is based on a real socket.
 *
 * Uses the [ByteReadChannel] and [ByteWriteChannel].
 */
class DefaultConnection(
    private val inputChannel: ByteReadChannel,
    private val outputChannel: ByteWriteChannel,
    override val remoteAddress: String,
    override val connectionScope: CoroutineScope
) : Connection {
    override var playerId: String = "[Undetermined]"
    override var playerName: String = "[Undetermined]"

    /**
     * Suspends until data becomes available on the input channel,
     * then reads it into a buffer.
     *
     * @return A [Pair] containing:
     *   - The number of bytes successfully read, or `-1` if the stream has ended.
     *   - A [ByteArray] containing the data that was read.
     */
    override suspend fun read(): Pair<Int, ByteArray> {
        val buffer = ByteArray(4096)
        val bytesRead = inputChannel.readAvailable(buffer, 0, buffer.size)
        if (bytesRead <= 0) return -1 to byteArrayOf()
        return bytesRead to buffer.copyOfRange(0, bytesRead)
    }

    /**
     * Send bytes [input] to client.
     *
     * No modification is done to the byte array is done.
     * Serialization must be done manually.
     */
    override suspend fun write(input: ByteArray, logOutput: Boolean, logFull: Boolean) {
        try {
            if (logOutput) {
                Logger.debug(logFull = logFull) {
                    buildString {
                        appendLine("[SOCKET SEND]")
                        appendLine("$LOG_INDENT_PREFIX raw       : ${input.safeAsciiString()}")
                        append("$LOG_INDENT_PREFIX raw (hex) : ${input.hexString()}")
                    }
                }
            }
            outputChannel.writeFully(input)
        } catch (e: Exception) {
            Logger.error { "Failed to send raw message to $remoteAddress: ${e.message}" }
            throw e
        }
    }

    override fun updatePlayerId(playerId: String) {
        this.playerId = playerId
    }

    /**
     * Shutdown the connection which cancels the [connectionScope].
     */
    override suspend fun shutdown() {
        try {
            connectionScope.cancel()
        } catch (e: Exception) {
            Logger.warn { "Exception during connection shutdown: ${e.message}" }
        }
    }

    override fun toString(): String {
        return "Connection(playerId=$playerId, playerName=$playerName, address=$remoteAddress)"
    }
}
