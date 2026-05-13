package encore.network.handler

import encore.datastore.collection.PlayerId
import encore.network.transport.Connection
import encore.network.fanchant.Fanchant

/**
 * Default implementation of [HandlerContext] where [sendRaw] is implemented
 * on a [Connection] object.
 */
class DefaultHandlerContext<T : Fanchant>(
    private val connection: Connection,
    override var playerId: PlayerId,
    override val fanchant: T
) : HandlerContext<T> {
    override suspend fun sendRaw(raw: ByteArray, logOutput: Boolean, logFull: Boolean) {
        connection.write(raw, logOutput, logFull)
    }

    override fun updatePlayerId(playerId: PlayerId) {
        connection.updatePlayerId(playerId)
        this.playerId = playerId
    }
}
