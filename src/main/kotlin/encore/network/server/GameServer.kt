package encore.network.server

import encore.context.ServerContext
import encore.fancam.Fancam
import encore.fancam.LOG_INDENT_PREFIX
import encore.network.transport.Connection
import encore.network.transport.DefaultConnection
import encore.network.handler.DefaultHandlerContext
import encore.network.messaging.format.DecodeResult
import encore.network.messaging.format.DefaultFormat
import encore.network.messaging.socket.SocketMessage
import encore.network.messaging.socket.SocketMessageDispatcher
import encore.subunit.scope.ServerScope
import encore.utils.hexString
import encore.utils.safeAsciiString
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.date.*
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

data class GameServerConfig(
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
        this.gameServerScope = scope
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

        val selectorManager = SelectorManager(Dispatchers.IO)
        gameServerScope.launch(Dispatchers.IO) {
            try {
                val serverSocket = aSocket(selectorManager).tcp().bind(config.host, config.port)

                while (isActive) {
                    val socket = serverSocket.accept()

                    val connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
                    val connection = DefaultConnection(
                        inputChannel = socket.openReadChannel(),
                        outputChannel = socket.openWriteChannel(autoFlush = true),
                        remoteAddress = socket.remoteAddress.toString(),
                        connectionScope = connectionScope
                    )
                    connection.registerOnSendHook {
                        serverContext.playerLifecycleHandler.onSend(serverContext, connection)
                    }

                    Fancam.info { "New client: ${connection.remoteAddress}" }
                    serverContext.playerLifecycleHandler.onConnect(serverContext, connection)

                    handleClient(connection)
                }
            } catch (_: CancellationException) {
                Fancam.info { "Game server coroutine cancelled (shutdown)" }
            } catch (e: Exception) {
                Fancam.error { "ERROR on server: $e" }
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

                    serverContext.playerLifecycleHandler.onReceive(serverContext, connection)
                    serverContext.subunits.activity.updateLastActivity(connection.playerId)

                    // start handle
                    var msgType = "[Undetermined]"
                    val elapsed = measureTimeMillis {
                        msgType = handleMessage(connection, data)
                    }

                    // end handle
                    Fancam.debug {
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
            } catch (_: CancellationException) {
                Fancam.info { "Coroutine cancalled for $connection" }
            } catch (e: Exception) {
                Fancam.error(e) { "Exception in client socket $connection" }
            } finally {
                Fancam.info { "Cleaning up for $connection" }
                serverContext.playerLifecycleHandler.onDisconnect(serverContext, connection)

                // Only perform cleanup if playerId is set (client was authenticated)
                if (connection.playerId != "[Undetermined]") {
                    serverContext.subunits.activity.markOffline(connection.playerId)
                    serverContext.subunits.account.updateLastActivity(connection.playerId, getTimeMillis())
                    serverContext.contextTracker.removeContext(connection.playerId)
                    serverContext.serverTaskDispatcher.stopAllTasksForPlayer(connection.playerId)
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
            Fancam.debug { "[SOCKET] Ignored empty byte array from connection=$connection" }
            return "[Empty data]"
        }

        Fancam.debug {
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
        val possibleFormats = serverContext.messageFormatRegistry.identifyFormat(data)

        // Find possible format for this message
        for (format in possibleFormats) {
            try {
                @Suppress("UNCHECKED_CAST")
                val result = format.tryDecode(data)

                if (result is DecodeResult.Success<*>) {
                    // Success decoding, convert to SocketMessage
                    val message = format.materializeAny(result.value)

                    Fancam.debug {
                        buildString {
                            appendLine("[SOCKET DECODE]")
                            appendLine("$LOG_INDENT_PREFIX type   : ${message.type()}")
                            append("$LOG_INDENT_PREFIX format : ${format.name}")
                        }
                    }

                    matched += format.name to message
                }
            } catch (e: Exception) {
                Fancam.error { "Decode error in format ${format.name}; e=$e" }
            }
        }

        // Allow only one interpretation of the message, if there is multiple
        val (chosenFormat, message) = matched.firstOrNull() ?: (defaultFormat to defaultMessage(data))
        if (matched.size > 1) {
            Fancam.warn {
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

    // when no other format matches, uses DefaultFormat
    private val defaultFormat = DefaultFormat()

    // which also produces default message from its tryDecode and materializeAny implementation
    private fun defaultMessage(data: ByteArray): SocketMessage {
        return defaultFormat.materializeAny(
            (defaultFormat.tryDecode(data) as DecodeResult.Success)
                .value
        )
    }

    override suspend fun shutdown() {
        running = false
        serverContext.contextTracker.shutdown()
        serverContext.subunits.activity.disband(ServerScope)
        serverContext.subunits.session.disband(ServerScope)
        serverContext.serverTaskDispatcher.shutdown()
        gameServerScope.cancel()
    }
}
