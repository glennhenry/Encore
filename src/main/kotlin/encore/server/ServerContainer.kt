package encore.server

import encore.context.ServerContext
import encore.server.core.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

/**
 * The main server that orchestrates all sub-servers.
 *
 * Provides a single entry point to initialize, start, and shut down all sub-servers.
 * Serves as the root coroutine context, shared by sub-servers and client connections.
 */
class ServerContainer(
    parentScope: CoroutineScope,
    private val servers: List<Server>,
    private val context: ServerContext
) {
    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)

    suspend fun initializeAll() {
        servers.forEach { it.initialize(scope, context) }
    }

    suspend fun startAll() {
        servers.forEach { it.start() }
    }

    suspend fun shutdownAll() {
        servers.forEach { it.shutdown() }
        job.cancel()
        job.join()
    }
}
