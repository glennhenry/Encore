package encore.network.stage

import encore.context.ServerContext
import encore.fancam.Fancam
import encore.fancam.INDENT
import encore.network.transport.Connection
import encore.network.transport.DefaultConnection
import encore.network.handler.DefaultHandlerContext
import encore.network.fanchant.guide.AllRounderFanchantGuide
import encore.network.fanchant.guide.DecodeResult
import encore.network.fanchant.guide.FanchantGuide
import encore.network.fanchant.Fanchant
import encore.network.fanchant.FanchantCoordinator
import encore.subunit.scope.ServerScope
import encore.time.TimeCenter
import encore.utils.hexString
import encore.utils.safeAsciiString
import encore.utils.support.className
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.ClosedByteChannelException
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

data class GameStageConfig(
    val host: String,
    val port: Int,
)

/**
 * Main game server (socket).
 *
 * @property config Server host and port configuration.
 * @property setup Contains necessary registration and setup across context,
 *                 such as message format and tasks registration.
 */
class GameStage(
    private val config: GameStageConfig,
    private val setup: (FanchantCoordinator, ServerContext) -> Unit
) : Stage {
    private lateinit var gameStageScope: CoroutineScope
    private lateinit var serverContext: ServerContext
    private val socketDispatcher = FanchantCoordinator()

    private var running = false
    override fun isRunning(): Boolean = running

    override suspend fun initialize(scope: CoroutineScope, context: ServerContext) {
        this.gameStageScope = scope
        this.serverContext = context
        setup(socketDispatcher, context)
    }

    override suspend fun start() {
        if (running) {
            Fancam.warn { "Game server is already running" }
            return
        }
        running = true

        Fancam.info { "Socket server listening on ${config.host}:${config.port}" }

        val serverSocket = bind()
        listenForConnections(serverSocket)
    }

    private suspend fun bind(): ServerSocket {
        val selectorManager = SelectorManager(Dispatchers.IO)
        val serverSocket = aSocket(selectorManager).tcp().bind(config.host, config.port)
        return serverSocket
    }

    private fun listenForConnections(serverSocket: ServerSocket) {
        gameStageScope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    val socket = serverSocket.accept()
                    val connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
                    val connection = DefaultConnection(
                        inputChannel = socket.openReadChannel(),
                        outputChannel = socket.openWriteChannel(autoFlush = true),
                        remoteAddress = socket.remoteAddress.toString(),
                        onSend = { serverContext.playerLifecycleHandler.onSend(serverContext, it) },
                        connectionScope = connectionScope
                    )
                    activateConnection(connection)
                }
            } catch (_: CancellationException) {
                Fancam.info { "Game server coroutine cancelled (shutdown)" }
            } catch (e: Exception) {
                Fancam.error { "ERROR on server: $e" }
            } finally {
                shutdown()
            }
        }
    }

    fun activateConnection(connection: Connection) {
        Fancam.info { "New $connection" }
        serverContext.playerLifecycleHandler.onConnect(serverContext, connection)
        processConnection(connection)
    }

    /**
     * Handle client [Connection] in suspending manner until data is available.
     */
    fun processConnection(connection: Connection) {
        connection.connectionScope.launch {
            try {
                loop@ while (isActive) {
                    val (bytesRead, data) = connection.read()
                    if (bytesRead <= 0) break@loop

                    serverContext.playerLifecycleHandler.onReceive(serverContext, connection)
                    serverContext.subunits.presence.updateLastActivity(connection.playerId)

                    // start handle
                    var fanchantType = "[Undetermined]"
                    val elapsed = measureTimeMillis {
                        fanchantType = handleFanchant(connection, data)
                    }

                    // end handle
                    Fancam.debug {
                        buildString {
                            appendLine("<===== [SOCKET END]")
                            appendLine("$INDENT type      : $fanchantType")
                            appendLine("$INDENT identity  : ${connection.identity}")
                            appendLine("$INDENT duration  : ${elapsed}ms")
                            append("====================================================================================================")
                        }
                    }
                }
            } catch (e: CancellationException) {
                Fancam.trace { "Coroutine cancellation for $connection: ${e.message}" }
            } catch (e: ClosedByteChannelException) {
                Fancam.info { "Connection closed for $connection: ${e.message}" }
            } catch (e: Exception) {
                Fancam.error(e) { "Exception in socket $connection" }
            } finally {
                Fancam.info { "Cleaning up for $connection" }
                serverContext.playerLifecycleHandler.onDisconnect(serverContext, connection)

                // Only perform cleanup if playerId is set (client was authenticated)
                if (connection.playerId != "[Undetermined]") {
                    serverContext.subunits.presence.markOffline(connection.playerId)
                    serverContext.subunits.account.updateLastActivity(connection.playerId, TimeCenter.system.now())
                    serverContext.contextRegistry.removeContext(connection.playerId)
                }

                connection.shutdown()
            }
        }
    }

    /**
     * Handle network message [data] in raw bytes received from [Connection] by:
     *
     * 1. Identify the [FanchantGuide] by calling [FanchantGuide.verify].
     * 2. Try to decode the format with [FanchantGuide.tryDecode].
     * 3. Call [FanchantGuide.materialize] into a high-level [Fanchant].
     * 4. Dispatch to the registered fanchant handlers.
     *
     * **Note**: By this architecture, it's possible for a single packet to be
     * successfully decoded by multiple fanchant guides. This situation is
     * inherently ambiguous. In such cases, the first successful decoding
     * result is selected, and a warning is logged.
     *
     * @return The specific fanchant type where decode succeed.
     */
    private suspend fun handleFanchant(connection: Connection, data: ByteArray): String {
        // Empty data
        if (data.isEmpty()) {
            Fancam.debug { "[SOCKET] Ignored empty data from connection=$connection" }
            return "[Empty data]"
        }

        Fancam.debug {
            buildString {
                appendLine("=====> [SOCKET RECEIVE]")
                appendLine("$INDENT identity  : ${connection.identity}")
                appendLine("$INDENT bytes     : ${data.size}")
                appendLine("$INDENT raw       : ${data.safeAsciiString()}")
                append("$INDENT raw (hex) : ${data.hexString()}")
            }
        }

        val matched = mutableListOf<Pair<String, Fanchant>>()
        val possibleGuides = serverContext.fanchantGuideRegistry.identify(data)

        // Find possible guide for this fanchant
        for (guide in possibleGuides) {
            try {
                val result = guide.tryDecode(data)

                if (result is DecodeResult.Success<*>) {
                    // Success decoding, convert to Fanchant
                    val fanchant = guide.materializeAny(result.value)

                    Fancam.debug {
                        buildString {
                            appendLine("[SOCKET DECODE]")
                            appendLine("$INDENT type   : ${fanchant.type.id}")
                            append("$INDENT guide  : ${guide.className()}")
                        }
                    }

                    matched += guide.className() to fanchant
                }
            } catch (e: Exception) {
                Fancam.error(e) { "Decode error in fanchant guide ${guide.className()}" }
            }
        }

        // Allow only one interpretation of the fanchant, if there is multiple
        val (chosenGuide, fanchant) = matched.firstOrNull()
            ?: (allRounderFanchantGuide to allRounderFanchant(data))

        if (matched.size > 1) {
            Fancam.warn {
                buildString {
                    appendLine(
                        "Multiple fanchant guides decoded the same packet: " +
                                matched.joinToString { "${it.first}/type=${it.second.type.id}" }
                    )
                    append("$INDENT chosen: $chosenGuide/type=${fanchant.type.id}")
                }
            }
        }

        // Dispatch fanchant to handler
        val handler = socketDispatcher.findHandler(fanchant)
        val context = DefaultHandlerContext(
            connection = connection,
            fanchant = fanchant
        )
        handler.handleUnsafe(context)

        return fanchant.type.id
    }

    // when no other guide matches, uses AllRounderFanchantGuide
    private val allRounderFanchantGuide = AllRounderFanchantGuide()

    // which also produces an all rounder fanchant from its tryDecode
    // and materializeAny implementation
    private fun allRounderFanchant(data: ByteArray): Fanchant {
        return allRounderFanchantGuide.materializeAny(
            (allRounderFanchantGuide.tryDecode(data) as DecodeResult.Success)
                .value
        )
    }

    override suspend fun shutdown() {
        running = false
        serverContext.subunits.presence.disband(ServerScope)
        serverContext.subunits.session.disband(ServerScope)
        serverContext.stageActDirector.shutdown()
        gameStageScope.cancel()
    }
}
