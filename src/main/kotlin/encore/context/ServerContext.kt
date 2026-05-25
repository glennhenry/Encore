package encore.context

import encore.account.AccountRepository
import encore.account.AccountSubunit
import encore.account.BlankAccountRepository
import encore.account.PlayerCreationSubunit
import encore.presence.PlayerPresenceSubunit
import encore.acts.ActIdStore
import encore.acts.StageActDirector
import encore.auth.AuthSubunit
import encore.backstage.command.CommandDispatcher
import encore.datastore.BlankDataStore
import encore.datastore.DataStore
import encore.datastore.collection.PlayerId
import encore.session.SessionSubunit
import encore.subunit.Subunit
import encore.subunit.scope.ServerScope
import encore.time.SystemTimekeeper
import encore.time.Timekeeper
import encore.websocket.WebSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Represents the **global server-side context**.
 *
 * `ServerContext` includes various server-side components needed on the server.
 * It acts as a dependency container which is distributed across the server code.
 *
 * @property dataStore [DataStore] instance of the server.
 * @property contextRegistry Tracks and manages [PlayerContext].
 * @property commandDispatcher Tracks and executes server commands.
 * @property stageActDirector Provide API to start and stop stage acts.
 * @property webSocketManager Manages client websocket connections.
 * @property subunits Container for server subunit instances.
 */
data class ServerContext(
    val dataStore: DataStore,
    val contextRegistry: ContextRegistry,
    val commandDispatcher: CommandDispatcher,
    val stageActDirector: StageActDirector,
    val webSocketManager: WebSocketManager,
    val subunits: ServerSubunits
) {
    companion object {
        /**
         * Creates a test instance of [ServerContext].
         *
         * @param parentScope `CoroutineScope` for [SessionSubunit].
         * @param timekeeper [Timekeeper] for [StageActDirector].
         * @param dataStore Also used to build [PlayerCreationSubunit].
         * @param accountRepository Used to build [AccountSubunit].
         * @param contextFactory Required by [ContextRegistry],
         *                       uses [FakeContextFactory] with empty map by default.
         */
        fun createForTest(
            parentScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
            timekeeper: Timekeeper = SystemTimekeeper,
            dataStore: DataStore = BlankDataStore(),
            accountRepository: AccountRepository = BlankAccountRepository(),
            contextFactory: ContextFactory = FakeContextFactory(emptyMap())
        ): ServerContext {
            val account = AccountSubunit(accountRepository)
            val session = SessionSubunit.createForTest(parentScope)
            val creation = PlayerCreationSubunit.createForTest(dataStore)

            return ServerContext(
                dataStore = dataStore,
                contextRegistry = ContextRegistry(contextFactory),
                commandDispatcher = CommandDispatcher(),
                stageActDirector = StageActDirector(timekeeper, ActIdStore),
                webSocketManager = WebSocketManager(),
                subunits = ServerSubunits(
                    account = account,
                    auth = AuthSubunit(account, creation, session),
                    creation = creation,
                    presence = PlayerPresenceSubunit(),
                    session = session
                )
            )
        }
    }
}

/**
 * Shorthand to retrieve [PlayerContext] of [playerId] from [ContextRegistry].
 *
 * @throws IllegalStateException if context is not found.
 */
fun ServerContext.requirePlayerContext(playerId: PlayerId): PlayerContext {
    return contextRegistry.getContext(playerId)
        ?: error("PlayerContext not found for playerId=$playerId")
}

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
 * @property creation Provides player creation mechanism.
 * @property presence Tracks player's presence.
 * @property session Manages session of players.
 */
data class ServerSubunits(
    val account: AccountSubunit,
    val auth: AuthSubunit,
    val creation: PlayerCreationSubunit,
    val presence: PlayerPresenceSubunit,
    val session: SessionSubunit,
) {
    /**
     * Return all server subunit instances.
     */
    fun all(): Set<Subunit<ServerScope>> {
        return setOf(account, auth, creation, presence, session)
    }
}
