package encore.network.handler

import encore.datastore.collection.PlayerId
import encore.network.transport.Connection
import encore.network.fanchant.Fanchant
import encore.network.transport.ConnectionIdentity

/**
 * Default implementation of [HandlerContext] where [sendRaw] is implemented
 * on a [Connection] object.
 */
class DefaultHandlerContext<T : Fanchant>(
    private val connection: Connection,
    override val fanchant: T
) : HandlerContext<T> {
    override val connectionIdentity: ConnectionIdentity = connection.identity

    override suspend fun sendRaw(raw: ByteArray, logOutput: Boolean, logFull: Boolean) {
        connection.write(raw, logOutput, logFull)
    }

    override fun acknowledge(playerId: PlayerId, username: String) {
        connection.acknowledge(playerId, username)
    }
}
