package server

import context.ServerContext
import server.core.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

/**
 * The main server that orchestrates all sub-servers.
 *
 * Provides a single entry point to initialize, start, and shut down all sub-servers.
 * Serves as the root coroutine context, shared by sub-servers and client connections.
 */
class ServerContainer(private val servers: List<Server>, private val context: ServerContext) {
    private val job = SupervisorJob()
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + job)

    suspend fun initializeAll() {
        servers.forEach { it.initialize(coroutineScope, context) }
    }

    suspend fun startAll() {
        servers.forEach { it.start() }
    }

    /**
     * To run `readln()` from terminal.
     */
    fun startAcceptingCommandInputs(blockingRead: suspend () -> Unit) {
        coroutineScope.launch { blockingRead() }
    }

    suspend fun shutdownAll() {
        servers.forEach { it.shutdown() }
        job.cancelAndJoin()
    }
}
