package bootstrap

import MongoCollectionName
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import encore.account.AccountSubunit
import encore.account.MongoAccountRepository
import encore.account.PlayerCreationSubunit
import encore.acts.ActIdStore
import encore.acts.StageActDirector
import encore.auth.AuthSubunit
import encore.backstage.command.CommandDispatcher
import encore.context.ContextRegistry
import encore.context.ServerContext
import encore.context.ServerSubunits
import encore.datastore.MongoCollectionName
import encore.datastore.MongoDataStore
import encore.network.lifecycle.PlayerLifecycleHandler
import encore.presence.PlayerPresenceSubunit
import encore.session.SessionSubunit
import encore.subunit.scope.ServerScope
import encore.time.TimeCenter
import encore.venue.Venue
import encore.websocket.WebSocketManager
import game.RealContextFactory
import kotlinx.coroutines.CoroutineScope

/**
 * Create and return a [ServerContext] instance.
 */
suspend fun createServerContext(
    appScope: CoroutineScope,
    serverSubunitScope: ServerScope,
    collectionName: MongoCollectionName,
    mongoClient: MongoClient,
    mongoDatabase: MongoDatabase
): ServerContext {
    // setup ServerContext
    val dataStore = MongoDataStore(
        db = mongoClient.getDatabase(Venue.encore.database.dbName),
        collectionName = MongoCollectionName
    )
    val accountRepository = MongoAccountRepository(
        accountCollection = mongoDatabase.getCollection(MongoCollectionName.playerAccount)
    )
    // RealContextFactory must be updated overtime to update PlayerSubunits construction
    val contextRegistry = ContextRegistry(
        factory = RealContextFactory(dataStore, collectionName, mongoDatabase)
    )
    val stageActDirector = StageActDirector(
        timeSource = TimeCenter.source,
        actStore = ActIdStore
    )
    val commandDispatcher = CommandDispatcher()
    val playerLifecycleHandler = PlayerLifecycleHandler(logEachHook = true)
    val webSocketManager = WebSocketManager()

    // setup ServerSubunits
    val accountSubunit = AccountSubunit(accountRepository)
    val playerPresenceSubunit = PlayerPresenceSubunit()
    val sessionSubunit = SessionSubunit(appScope, TimeCenter.source)
    val playerCreationSubunit = PlayerCreationSubunit(dataStore)
    val authSubunit = AuthSubunit(accountSubunit, playerCreationSubunit, sessionSubunit)

    val subunits = ServerSubunits(
        account = accountSubunit,
        presence = playerPresenceSubunit,
        auth = authSubunit,
        session = sessionSubunit,
        creation = playerCreationSubunit
    )

    // debut all subunits
    subunits.debut(ServerScope)

    val serverContext = ServerContext(
        dataStore = dataStore,
        contextRegistry = contextRegistry,
        stageActDirector = stageActDirector,
        commandDispatcher = commandDispatcher,
        playerLifecycleHandler = playerLifecycleHandler,
        webSocketManager = webSocketManager,
        subunits = subunits
    )

    return serverContext
}
