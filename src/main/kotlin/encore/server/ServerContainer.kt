package encore.server

import encore.context.ServerContext
import encore.server.core.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job

/**
 * The main server that orchestrates all sub-servers.
 *
 * Provides a single entry point to initialize, start, and shut down all sub-servers.
 * Serves as the root coroutine context, shared by sub-servers and client connections.
 */
class ServerContainer(
    private val coroutineScope: CoroutineScope,
    private val servers: List<Server>,
    private val context: ServerContext
) {
    suspend fun initializeAll() {
        servers.forEach { it.initialize(coroutineScope, context) }
    }

    suspend fun startAll() {
        servers.forEach { it.start() }
    }

    suspend fun shutdownAll() {
        servers.forEach { it.shutdown() }
        coroutineScope.coroutineContext.job.cancelAndJoin()
    }
}
