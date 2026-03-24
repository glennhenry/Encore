package encore.context

import encore.core.PlayerService
import encore.core.ServerService
import encore.db.Database
import encore.db.EmptyDatabase
import encore.devtools.command.core.CommandDispatcher
import encore.server.core.OnlinePlayerRegistry
import encore.server.messaging.format.MessageFormatRegistry
import encore.server.tasks.ServerTaskDispatcher
import encore.user.EmptyPlayerAccountRepository
import encore.user.PlayerAccountRepository
import encore.user.auth.AuthProvider
import encore.user.auth.EmptyAuthProvider
import encore.user.auth.SessionManager
import encore.ws.WebSocketManager

/**
 * Represents the **global server-side context** which includes various server components.
 *
 * @property db [Database] instance of the server.
 * @property playerAccountRepository Repository class that holds player accounts.
 * @property sessionManager Manages session of players.
 * @property authProvider Provides authentication functions.
 * @property onlinePlayerRegistry Keep tracks online status of each player.
 * @property contextTracker Tracks and manages each player's context.
 * @property formatRegistry Track the known message format and registered codecs
 *                           for network messages.
 * @property taskDispatcher Provide API to start and stop server-sided task.
 * @property commandDispatcher Tracks and executes server commands.
 * @property wsManager Manages client websocket connections.
 * @property services Container for server services instance.
 */
data class ServerContext(
    val db: Database,
    val playerAccountRepository: PlayerAccountRepository,
    val sessionManager: SessionManager,
    val authProvider: AuthProvider,
    val onlinePlayerRegistry: OnlinePlayerRegistry,
    val contextTracker: ContextTracker,
    val formatRegistry: MessageFormatRegistry,
    val taskDispatcher: ServerTaskDispatcher,
    val commandDispatcher: CommandDispatcher,
    val wsManager: WebSocketManager,
    val services: ServerServices
) {
    companion object {
        /**
         * Create a fake, simple to use [ServerContext] for testing purposes.
         *
         * It allows injection of interface-based dependencies such as [Database],
         * [PlayerAccountRepository], and [AuthProvider].
         *
         * By default, the [FakeContextTracker] is used, while all other components
         * (e.g. [SessionManager], [OnlinePlayerRegistry], [MessageFormatRegistry], and
         * [ServerTaskDispatcher]) are initialized with their default implementations.
         */
        fun fake(
            db: Database = EmptyDatabase(),
            playerAccountRepository: PlayerAccountRepository = EmptyPlayerAccountRepository(),
            authProvider: AuthProvider = EmptyAuthProvider(),
            contextTracker: ContextTracker = FakeContextTracker()
        ): ServerContext {
            return ServerContext(
                db = db,
                playerAccountRepository = playerAccountRepository,
                sessionManager = SessionManager(),
                authProvider = authProvider,
                onlinePlayerRegistry = OnlinePlayerRegistry(),
                contextTracker = contextTracker,
                formatRegistry = MessageFormatRegistry(),
                taskDispatcher = ServerTaskDispatcher(),
                commandDispatcher = CommandDispatcher(),
                wsManager = WebSocketManager(),
                services = ServerServices()
            )
        }
    }
}

/**
 * Retrieve the [PlayerContext] of [playerId].
 *
 * @return `null` if context is not found.
 */
fun ServerContext.getPlayerContextOrNull(playerId: String): PlayerContext? =
    contextTracker.getContext(playerId)

/**
 * Retrieve the non-null [PlayerContext] of [playerId].
 *
 * @throws IllegalStateException if context is not found.
 */
fun ServerContext.requirePlayerContext(playerId: String): PlayerContext =
    getPlayerContextOrNull(playerId)
        ?: error("PlayerContext not found for playerId=$playerId")

/**
 * A container that holds all **global service instances** used by the server.
 *
 * Whereas [PlayerService] encapsulates domain logic specific to individual players,
 * [ServerService] encapsulates server-wide domain logic that operates on shared data.
 *
 * For example, a leaderboard is not owned by any single player — it represents
 * global state managed by the server. A `LeaderboardService` might provide operations
 * to retrieve player rankings or update them.
 */
data class ServerServices(
    val example: String = ""
)
