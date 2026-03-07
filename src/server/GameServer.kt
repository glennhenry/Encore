package server

import SERVER_ADDRESS
import SERVER_SOCKET_PORT
import context.ServerContext
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.date.*
import kotlinx.coroutines.*
import server.core.Server
import server.core.network.Connection
import server.core.network.DefaultConnection
import server.handler.DefaultHandlerContext
import server.messaging.format.DecodeResult
import server.messaging.socket.SocketMessage
import server.messaging.socket.SocketMessageDispatcher
import utils.functions.hexString
import utils.functions.safeAsciiString
import utils.logging.Logger
import utils.logging.Logger.LOG_INDENT_PREFIX
import kotlin.system.measureTimeMillis

data class GameServerConfig(
    val host: String = SERVER_ADDRESS,
    val port: Int = SERVER_SOCKET_PORT,
)

/**
 * Main game server (socket).
 *
 * @property config Server host and port configuration.
 * @property setup Contains necessary registration and setup across context,
 *                 such as message format and tasks registration.
 */
class GameServer(
    private val config: GameServerConfig,
    private val setup: (SocketMessageDispatcher, ServerContext) -> Unit
) : Server {
    override val name: String = "GameServer"

    private lateinit var gameServerScope: CoroutineScope
    private lateinit var serverContext: ServerContext
    private val socketDispatcher = SocketMessageDispatcher()

    private var running = false
    override fun isRunning(): Boolean = running

    override suspend fun initialize(scope: CoroutineScope, context: ServerContext) {
        this.gameServerScope = CoroutineScope(scope.coroutineContext + SupervisorJob() + Dispatchers.IO)
        this.serverContext = context
        setup(socketDispatcher, context)
    }

    override suspend fun start() {
        if (running) {
            Logger.warn { "Game server is already running" }
            return
        }
        running = true

        Logger.info { "Socket server listening on ${config.host}:${config.port}" }

        val selectorManager = SelectorManager(Dispatchers.IO)
        gameServerScope.launch {
            try {
                val serverSocket = aSocket(selectorManager).tcp().bind(config.host, config.port)

                while (isActive) {
                    val socket = serverSocket.accept()
                    val connection = DefaultConnection(
                        inputChannel = socket.openReadChannel(),
                        outputChannel = socket.openWriteChannel(autoFlush = true),
                        remoteAddress = socket.remoteAddress.toString(),
                        connectionScope = CoroutineScope(gameServerScope.coroutineContext + SupervisorJob() + Dispatchers.Default),
                    )
                    Logger.info { "New client: ${connection.remoteAddress}" }
                    handleClient(connection)
                }
            } catch (e: CancellationException) {
                Logger.debug { "Game server coroutine cancelled (shutdown)" }
                throw e
            } catch (e: Exception) {
                Logger.error { "ERROR on server: $e" }
                shutdown()
            }
        }
    }

    /**
     * Handle client [Connection] in suspending manner until data is available.
     */
    fun handleClient(connection: Connection) {
        connection.connectionScope.launch {
            try {
                loop@ while (isActive) {
                    val (bytesRead, data) = connection.read()
                    if (bytesRead <= 0) break@loop

                    serverContext.onlinePlayerRegistry.updateLastActivity(connection.playerId)

                    // start handle
                    var msgType = "[Undetermined]"
                    val elapsed = measureTimeMillis {
                        msgType = handleMessage(connection, data)
                    }

                    // end handle
                    Logger.debug {
                        buildString {
                            appendLine("<===== [SOCKET END]")
                            appendLine("$LOG_INDENT_PREFIX type      : $msgType")
                            appendLine("$LOG_INDENT_PREFIX playerId  : ${connection.playerId}")
                            if (connection.playerId == "[Undetermined]") {
                                appendLine("$LOG_INDENT_PREFIX address   : ${connection.remoteAddress}")
                            }
                            appendLine("$LOG_INDENT_PREFIX duration  : ${elapsed}ms")
                            append("====================================================================================================")
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.error { "Exception in client socket $connection: $e" }
            } finally {
                Logger.info { "Cleaning up for $connection" }

                // Only perform cleanup if playerId is set (client was authenticated)
                if (connection.playerId != "[Undetermined]") {
                    serverContext.onlinePlayerRegistry.markOffline(connection.playerId)
                    serverContext.playerAccountRepository.updateLastLogin(connection.playerId, getTimeMillis())
                    serverContext.contextTracker.removeContext(connection.playerId)
                    serverContext.taskDispatcher.stopAllTasksForPlayer(connection.playerId)
                }

                connection.shutdown()
            }
        }
    }

    /**
     * Handle message from [Connection] with raw bytes [data] by:
     *
     * 1. Identify the message format.
     * 2. Try to decode the format.
     * 3. Materialize into a high-level [SocketMessage].
     * 4. Dispatch to registered message handlers.
     *
     * ```
     * bytes
     *   ↓ (identifyFormat)
     * formatCandidates
     *   ↓ (tryDecode)
     * DecodeResult
     *   ↓ (materialize)
     * SocketMessage
     *   ↓ (findHandlerFor)
     * handler.handle()
     * ```
     *
     * **Note**: By this architecture, it's possible for a single packet to be
     * successfully decoded by multiple message formats. This situation is
     * inherently ambiguous. In such cases, the first successful decoding
     * result is selected, and a warning is logged.
     *
     * @return The various types of message decoded successfully, used merely
     *         to mark the end of socket dispatchment.
     */
    private suspend fun handleMessage(connection: Connection, data: ByteArray): String {
        // Empty data
        if (data.isEmpty()) {
            Logger.debug { "[SOCKET] Ignored empty byte array from connection=$connection" }
            return "[Empty data]"
        }

        Logger.debug {
            buildString {
                appendLine("=====> [SOCKET RECEIVE]")
                appendLine("$LOG_INDENT_PREFIX playerId  : ${connection.playerId}")
                if (connection.playerId == "[Undetermined]") {
                    appendLine("$LOG_INDENT_PREFIX address   : ${connection.remoteAddress}")
                }
                appendLine("$LOG_INDENT_PREFIX bytes     : ${data.size}")
                appendLine("$LOG_INDENT_PREFIX raw       : ${data.safeAsciiString()}")
                append("$LOG_INDENT_PREFIX raw (hex) : ${data.hexString()}")
            }
        }

        val matched = mutableListOf<Pair<String, SocketMessage>>()
        val possibleFormats = serverContext.formatRegistry.identifyFormat(data)

        // Find possible format for this message
        for (format in possibleFormats) {
            try {
                @Suppress("UNCHECKED_CAST")
                val result = format.tryDecode(data)

                if (result is DecodeResult.Success<*>) {
                    // Success decoding, convert to SocketMessage
                    val message = format.materializeAny(result.value)

                    Logger.debug {
                        buildString {
                            appendLine("[SOCKET DECODE]")
                            appendLine("$LOG_INDENT_PREFIX type   : ${message.type()}")
                            append("$LOG_INDENT_PREFIX format : ${format.name}")
                        }
                    }

                    matched += format.name to message
                }
            } catch (e: Exception) {
                Logger.error { "Decode error in format ${format.name}; e=$e" }
            }
        }

        // Allow only one interpretation of the message, if there is multiple
        val (chosenFormat, message) = matched.first()

        if (matched.size > 1) {
            Logger.warn {
                buildString {
                    appendLine(
                        "Multiple formats decoded the same packet: " +
                                matched.joinToString { "${it.first}/type=${it.second.type()}" }
                    )
                    append("$LOG_INDENT_PREFIX chosen: $chosenFormat/type=${message.type()}")
                }
            }
        }

        // Dispatch message to handler
        socketDispatcher.findHandlerFor(message).forEach { handler ->
            val context = DefaultHandlerContext(
                connection = connection,
                playerId = connection.playerId,
                message = message
            )

            handler.handleUnsafe(context)
        }

        return message.type()
    }

    override suspend fun shutdown() {
        running = false
        serverContext.contextTracker.shutdown()
        serverContext.onlinePlayerRegistry.shutdown()
        serverContext.sessionManager.shutdown()
        serverContext.taskDispatcher.shutdown()
        gameServerScope.cancel()
    }
}
