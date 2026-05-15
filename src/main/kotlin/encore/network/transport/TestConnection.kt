package encore.network.transport

import encore.datastore.collection.PlayerId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

/**
 * Implementation of [Connection] which is used for testing purposes.
 *
 * This allows test code to inject incoming messages of raw bytes
 * and capture all outgoing writes.
 *
 * Example:
 * ```
 * val conn = TestConnection(
 *     connectionScope = CoroutineScope(StandardTestDispatcher()),
 *     playerId = "p1",
 *     playerName = "Alice"
 * )
 * conn.enqueueIncoming("Hello".toByteArray())
 *
 * val (_, bytes) = conn.read() // returns the injected message
 * assertEquals("Hello", String(bytes))
 *
 * conn.write("World".toByteArray())
 * assertEquals("World", String(conn.getOutgoing().first()))
 * ```
 */
class TestConnection(
    override val identity: ConnectionIdentity,
    override val connectionScope: CoroutineScope
) : Connection {
    private val incoming = Channel<ByteArray>(Channel.UNLIMITED)
    private val writtenBytes = mutableListOf<ByteArray>()

    /**
     * Enqueue a raw [ByteArray] to be returned by [Connection.read].
     */
    fun enqueueIncoming(data: ByteArray) {
        incoming.trySend(data)
    }

    /**
     * Get the outgoing bytes which is written by [Connection.write].
     */
    fun getOutgoing(): List<ByteArray> {
        return writtenBytes
    }

    /**
     * To wait until outgoing is ready (non-empty) for one minute,
     * delaying every [timeoutInSeconds].
     */
    suspend fun awaitOutgoing(timeoutInSeconds: Int = 1) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            while (getOutgoing().isEmpty()) {
                delay(timeoutInSeconds.seconds)
            }
        }
    }

    override suspend fun read(): Pair<Int, ByteArray> {
        val bytes = incoming.receiveCatching().getOrNull() ?: return -1 to byteArrayOf()
        return bytes.size to bytes
    }

    override suspend fun write(input: ByteArray, logOutput: Boolean, logFull: Boolean) {
        writtenBytes += input
    }

    override fun acknowledge(playerId: PlayerId, username: String) {
        identity.playerId = playerId
        identity.username = username
    }

    override suspend fun shutdown() {
        incoming.close()
    }

    override fun toString(): String = "TestConnection(${this.identity})"
}
