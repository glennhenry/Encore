package encore.context

import encore.datastore.DataStore
import encore.datastore.BlankDataStore
import encore.backstage.command.CommandDispatcher
import encore.datastore.collection.PlayerId
import encore.server.core.OnlinePlayerRegistry
import encore.server.messaging.format.MessageFormatRegistry
import encore.server.tasks.ServerTaskDispatcher
import encore.subunit.Subunit
import encore.subunit.scope.ServerScope
import encore.account.BlankAccountRepository
import encore.account.AccountRepository
import encore.account.AccountSubunit
import encore.account.PlayerCreationSubunit
import encore.auth.AuthSubunit
import encore.session.SessionSubunit
import encore.ws.WebSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Represents the **global server-side context** which includes various server components.
 *
 * @property dataStore [DataStore] instance of the server.
 * @property onlinePlayerRegistry Keep tracks online status of each player.
 * @property contextTracker Tracks and manages each player's context.
 * @property formatRegistry Track the known message format and registered codecs
 *                           for network messages.
 * @property taskDispatcher Provide API to start and stop server-sided task.
 * @property commandDispatcher Tracks and executes server commands.
 * @property wsManager Manages client websocket connections.
 * @property subunits Container for server subunit instances.
 */
data class ServerContext(
    val dataStore: DataStore,
    val onlinePlayerRegistry: OnlinePlayerRegistry,
    val contextTracker: ContextTracker,
    val formatRegistry: MessageFormatRegistry,
    val taskDispatcher: ServerTaskDispatcher,
    val commandDispatcher: CommandDispatcher,
    val wsManager: WebSocketManager,
    val subunits: ServerSubunits
) {
    companion object {
        /**
         * Create a fake, simple to use [ServerContext] for testing purposes.
         *
         * It allows injection of interface-based dependencies such as [DataStore],
         * [AccountRepository], and [AuthProvider].
         *
         * By default, the [FakeContextTracker] is used, while all other components
         * (e.g. [SessionSubunit], [OnlinePlayerRegistry], [MessageFormatRegistry], and
         * [ServerTaskDispatcher]) are initialized with their default implementations.
         */
        fun fake(
            parentScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
            dataStore: DataStore = BlankDataStore(),
            accountRepository: AccountRepository = BlankAccountRepository(),
            contextTracker: ContextTracker = FakeContextTracker()
        ): ServerContext {
            val account = AccountSubunit(accountRepository)
            val session = SessionSubunit.createForTest(parentScope)
            val creation = PlayerCreationSubunit.createForTest(dataStore)

            return ServerContext(
                dataStore = dataStore,
                onlinePlayerRegistry = OnlinePlayerRegistry(),
                contextTracker = contextTracker,
                formatRegistry = MessageFormatRegistry(),
                taskDispatcher = ServerTaskDispatcher(),
                commandDispatcher = CommandDispatcher(),
                wsManager = WebSocketManager(),
                subunits = ServerSubunits(
                    account = account,
                    auth = AuthSubunit(account, creation, session),
                    session = session,
                    creation = creation
                )
            )
        }
    }
}

/**
 * Retrieve the [PlayerContext] of [playerId].
 *
 * @return `null` if context is not found.
 */
fun ServerContext.getPlayerContextOrNull(playerId: PlayerId): PlayerContext? =
    contextTracker.getContext(playerId)

/**
 * Retrieve the non-null [PlayerContext] of [playerId].
 *
 * @throws IllegalStateException if context is not found.
 */
fun ServerContext.requirePlayerContext(playerId: PlayerId): PlayerContext =
    getPlayerContextOrNull(playerId)
        ?: error("PlayerContext not found for playerId=$playerId")

/**
 * Container for all server-scoped [Subunit] instances.
 *
 * Server subunits encapsulate domain logic that operates at the server level.
 * They may manage shared state or provide global domain functionality,
 * with or without persistent data.
 *
 * Server subunits are typically bound to [ServerScope].
 *
 * Examples:
 * - An infra-related component providing session creation and verification.
 * - A leaderboard representing global state is not owned by any single player.
 *   A `LeaderboardSubunit` may expose operations to query or update rankings.
 * - A matchmaking system may not persist data, but can maintain in-memory
 *   state and provide matchmaking-specific functionality.
 *
 * @property account Provides API related to accounts.
 * @property auth Provides authentication functions.
 * @property session Manages session of players.
 * @property creation Provides player creation mechanism.
 */
data class ServerSubunits(
    val account: AccountSubunit,
    val auth: AuthSubunit,
    val session: SessionSubunit,
    val creation: PlayerCreationSubunit,
)
