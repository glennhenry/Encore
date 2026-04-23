package encore.network.transport

import encore.datastore.collection.PlayerId
import encore.fancam.Fancam
import encore.fancam.LOG_INDENT_PREFIX
import encore.fancam.events.Level
import encore.network.lifecycle.PlayerLifecycleHandler
import encore.utils.hexString
import encore.utils.safeAsciiString
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlin.coroutines.cancellation.CancellationException

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
    override var playerId: PlayerId = "[Undetermined]"
    override var playerName: String = "[Undetermined]"
    private var onSendHook: () -> Unit = {}

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
                Fancam.event(Level.Debug)
                    .message {
                        buildString {
                            appendLine("[SOCKET SEND]")
                            appendLine("$LOG_INDENT_PREFIX raw       : ${input.safeAsciiString()}")
                            append("$LOG_INDENT_PREFIX raw (hex) : ${input.hexString()}")
                        }
                    }
                    .log(full = logFull)
            }
            outputChannel.writeFully(input)
            onSendHook()
        } catch (e: Exception) {
            Fancam.error { "Failed to send raw message to $remoteAddress: ${e.message}" }
            throw e
        }
    }

    override fun updatePlayerId(playerId: PlayerId) {
        this.playerId = playerId
    }

    /**
     * Used to attach the [PlayerLifecycleHandler]'s `onSend` hook
     * within the connection transport internal.
     */
    fun registerOnSendHook(hook: () -> Unit) {
        onSendHook = hook
    }

    /**
     * Shutdown the connection which cancels the [connectionScope].
     */
    override suspend fun shutdown() {
        try {
            connectionScope.cancel(CancellationException("Connection closed"))
        } catch (e: Exception) {
            Fancam.warn { "Exception during connection shutdown: ${e.message}" }
        }
    }

    override fun toString(): String {
        return "Connection(playerId=$playerId, playerName=$playerName, address=$remoteAddress)"
    }
}
