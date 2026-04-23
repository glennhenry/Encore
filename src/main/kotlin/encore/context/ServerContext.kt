package encore.context

import encore.datastore.DataStore
import encore.datastore.BlankDataStore
import encore.backstage.command.CommandDispatcher
import encore.datastore.collection.PlayerId
import encore.activity.PlayerActivitySubunit
import encore.network.messaging.format.MessageFormatRegistry
import encore.tasks.ServerTaskDispatcher
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
 * @property contextTracker Tracks and manages each player's context.
 * @property messageFormatRegistry Track the known message format and registered codecs
 *                           for network messages.
 * @property serverTaskDispatcher Provide API to start and stop server-sided task.
 * @property commandDispatcher Tracks and executes server commands.
 * @property webSocketManager Manages client websocket connections.
 * @property subunits Container for server subunit instances.
 */
data class ServerContext(
    val dataStore: DataStore,
    val contextTracker: ContextTracker,
    val messageFormatRegistry: MessageFormatRegistry,
    val serverTaskDispatcher: ServerTaskDispatcher,
    val commandDispatcher: CommandDispatcher,
    val webSocketManager: WebSocketManager,
    val subunits: ServerSubunits
) {
    companion object {
        /**
         * Creates a test instance of [ServerContext].
         *
         * @param parentScope `CoroutineScope` for [SessionSubunit].
         * @param dataStore Also used to build [PlayerCreationSubunit].
         * @param accountRepository Used to build [AccountSubunit].
         * @param contextTracker Use [FakeContextTracker] by default.
         */
        fun createForTest(
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
                contextTracker = contextTracker,
                messageFormatRegistry = MessageFormatRegistry(),
                serverTaskDispatcher = ServerTaskDispatcher(),
                commandDispatcher = CommandDispatcher(),
                webSocketManager = WebSocketManager(),
                subunits = ServerSubunits(
                    account = account,
                    activity = PlayerActivitySubunit(),
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
 * @property activity Tracks player's activity status.
 * @property auth Provides authentication functions.
 * @property session Manages session of players.
 * @property creation Provides player creation mechanism.
 */
data class ServerSubunits(
    val account: AccountSubunit,
    val activity: PlayerActivitySubunit,
    val auth: AuthSubunit,
    val session: SessionSubunit,
    val creation: PlayerCreationSubunit,
)
