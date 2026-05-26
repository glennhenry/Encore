import bootstrap.*
import encore.EncoreIdentity
import encore.backstage.BackstageRoutes
import encore.backstage.command.ExampleCommand
import encore.context.ServerContext
import encore.datastore.MongoCollectionName
import encore.definition.GameReference
import encore.fancam.Fancam
import encore.network.lifecycle.PlayerLifecycle
import encore.network.stage.GameStage
import encore.network.stage.GameStageInitContext
import encore.network.stage.Stage
import encore.route.guard.DefaultSecurity
import encore.subunit.scope.PlayerScope
import encore.subunit.scope.ServerScope
import encore.time.TimeCenter
import encore.time.Timekeeper
import encore.time.source.SystemTimeSource
import encore.venue.Venue
import encore.websocket.handler.WsCommandHandler
import game.GameIdentity
import game.Globals
import game.fileRoutes
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.modules.SerializersModule
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

fun main() {
    Venue.prepare()

    // override Ktor dev mode with the framework custom config
    System.setProperty("io.ktor.development", Venue.encore.devMode.toString())

    embeddedServer(
        factory = Netty,
        host = Venue.encore.server.host,
        port = Venue.encore.server.port,
        watchPaths = listOf("classes")
    ) { configureApplication() }.start(wait = true)
}

val MongoCollectionName = MongoCollectionName(
    playerAccount = "player_account",
    playerObjects = "player_objects",
    playerServerObjects = "player_server_objects",
    serverObjects = "server_objects"
)

val SystemTimezone: ZoneId = ZoneId.systemDefault()

/**
 * Main configuration and wiring code for the application.
 */
suspend fun Application.configureApplication() {
    // install system time
    TimeCenter.update(
        system = Timekeeper(SystemTimeSource()),
        game = Timekeeper(SystemTimeSource())
    )

    // configure security
    val bannedAddresses = mutableSetOf<String>()
    val security = DefaultSecurity(bannedAddresses, TimeCenter.system)

    // setup the framework
    val (mongoc, db) = installEncore(
        module = SerializersModule { },
        security = security
    )

    // creates a coroutine scope for the app
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val serverSubunitScope = ServerScope

    // create server context
    val serverContext = createServerContext(
        appScope = appScope,
        serverSubunitScope = serverSubunitScope,
        mongoClient = mongoc,
        mongoDatabase = db
    )

    // create admin account
    if (Venue.encore.adminEnabled) {
        serverContext.subunits.creation.createAdmin(Globals, alwaysRecreate = false)
    }

    // register handlers for WebSocket
    websocketHandlers(serverContext)

    // register commands
    commandHandlers(serverContext)

    // initialize GameReference and register definitions
    gameReference()

    // configure routing
    // ephemeral token storage for /backstage entry
    val backstageToken = ConcurrentHashMap<String, Long>()

    // install routes
    routing {
        fileRoutes()
        with(BackstageRoutes(serverContext, backstageToken)) { install() }
    }

    // log startup
    logStartupInformation()

    // initialize game server
    val gameStage = GameStage(
        host = Venue.encore.server.host,
        port = Venue.encore.server.socketPort
    ) {
        lifecycleHooks()
        fanchantGuides()
        handlers()
    }

    val servers = buildList<Stage> {
        add(gameStage)
    }

    // initialize and start all the serverr
    servers.forEach { server ->
        server.initialize(appScope, serverContext)
        server.start()
    }

    // starts accepting terminal input
    acceptsTerminalInput(appScope, backstageToken)

    // prints encore banner
    println(EncoreIdentity.banner(GameIdentity))

    // install shutdown hook
    shutdownHook(appScope, serverSubunitScope, serverContext.subunits, servers)
}

fun websocketHandlers(serverContext: ServerContext) {
    with(serverContext.webSocketManager) {
        registerHandler(WsCommandHandler(serverContext))
    }
}

fun commandHandlers(serverContext: ServerContext) {
    with(serverContext.commandDispatcher) {
        register(ExampleCommand())
    }
}

fun gameReference() {
    GameReference.initialize {
        // add()
    }
}

fun GameStageInitContext.lifecycleHooks() {
    // register player lifecycle hooks...

    hook(PlayerLifecycle.OnReceive, "Update activity") { serverContext, connection ->
        serverContext.subunits.presence.updateLastActivity(connection.playerId)
    }

    hook(PlayerLifecycle.OnDisconnect, "Player cleanup") { serverContext, connection ->
        // Only perform cleanup if playerId is set (player was authenticated)
        val pid = connection.playerId
        if (pid != "[Undetermined]") {
            serverContext.subunits.presence.markOffline(pid)
            serverContext.subunits.account.updateLastActivity(pid, TimeCenter.system.now())
            serverContext.contextRegistry.getContext(pid)
                ?.subunits
                ?.all()
                ?.forEach { it.disband(PlayerScope(pid)) }
            serverContext.contextRegistry.removeContext(pid)
        }
    }
}

fun GameStageInitContext.fanchantGuides() {
    // register fanchant guides...

    // guide()
}

fun GameStageInitContext.handlers() {
    // register handlers

    // handler()
}
