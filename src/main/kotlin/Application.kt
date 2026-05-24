import com.mongodb.kotlin.client.coroutine.MongoClient
import encore.EncoreIdentity
import encore.account.AccountSubunit
import encore.account.MongoAccountRepository
import encore.account.PlayerCreationSubunit
import encore.acts.ActIdStore
import encore.acts.StageActDirector
import encore.auth.AuthSubunit
import encore.backstage.BackstageRoutes
import encore.backstage.command.CommandDispatcher
import encore.backstage.command.ExampleCommand
import encore.context.ContextRegistry
import encore.context.ServerContext
import encore.context.ServerSubunits
import encore.datastore.MongoCollectionName
import encore.datastore.MongoDataStore
import encore.definition.GameReference
import encore.fancam.Fancam
import encore.fancam.Tags
import encore.fancam.impl.OfficialFancam
import encore.network.fanchant.guide.FanchantGuide
import encore.network.fanchant.guide.FanchantGuideRegistry
import encore.network.lifecycle.PlayerLifecycleHandler
import encore.network.stage.GameStage
import encore.network.stage.GameStageConfig
import encore.network.stage.Stage
import encore.presence.PlayerPresenceSubunit
import encore.route.RouteHandler
import encore.route.guard.DefaultSecurity
import encore.route.guard.GuardResult
import encore.route.guard.SecurityGuard
import encore.route.interceptResponse
import encore.route.stringifyHttpRequest
import encore.serialization.JSON
import encore.session.SessionSubunit
import encore.time.source.SystemTimeSource
import encore.time.TimeCenter
import encore.time.Timekeeper
import encore.utils.identifier.Ids
import encore.venue.Venue
import encore.websocket.WebSocketManager
import encore.websocket.handler.WsCommandHandler
import game.Globals
import game.GameIdentity
import game.RealContextFactory
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.bson.Document
import java.io.File
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val MongoCollectionName = MongoCollectionName(
    playerAccount = "player_account",
    playerObjects = "player_objects",
    playerServerObjects = "player_server_objects",
    serverObjects = "server_objects"
)

val SystemTimezone: ZoneId = ZoneId.systemDefault()

fun main() {
    Venue.prepare()

    // override Ktor dev mode with framework custom config
    System.setProperty("io.ktor.development", Venue.encore.devMode.toString())

    embeddedServer(
        factory = Netty,
        host = Venue.encore.server.host,
        port = Venue.encore.server.port,
        watchPaths = listOf("classes")
    ) {
        module()
    }.start(wait = true)
}

suspend fun Application.module() {
    /* 1. Setup serialization */
    val module = SerializersModule {}
    val json = Json {
        serializersModule = module
        classDiscriminator = "_t"
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    @OptIn(ExperimentalSerializationApi::class)
    install(ContentNegotiation) {
        json(json)
    }
    JSON.initialize(json)
    // Protobuf.initialize(ProtoBuf)

    /* 2. Setup logger */
    Fancam.initialize(OfficialFancam(Venue.encore.fancam))

    /* 3. Install CORS */
    install(CORS) {
        anyHost() // change this on production
        allowHeader(HttpHeaders.ContentType)
        allowHeaders { true }
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }

    /* 5. Install status pages */
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            Fancam.error(cause, Tags.Api) { "Internal server scandal: ${call.request.httpMethod} ${call.request.uri}: ${cause.message}" }

            val message = if (this@module.developmentMode) {
                cause.stackTrace.joinToString("\n")
            } else {
                "Stage was sabotaged... T_T"
            }

            call.respondText(
                text = errorHtml(500, "<pre>${message}</pre>"),
                contentType = ContentType.Text.Html,
                status = HttpStatusCode.InternalServerError
            )
        }

        unhandled { call ->
            Fancam.warn(Tags.Api) { call.stringifyHttpRequest(unhandled = true) }

            call.respondText(
                text = errorHtml(404, "Not found in the system."),
                contentType = ContentType.Text.Html,
                status = HttpStatusCode.NotFound
            )
        }
    }

    /* 6. Configure Database */
    val mongoc = MongoClient.create(Venue.encore.database.dbUrlProd)
    val db = mongoc.getDatabase("admin")
    val commandResult = db.runCommand(Document("ping", 1))
    Fancam.info(Tags.Startup) { "MongoDB connection successful: $commandResult" }

    /* 7. Install websockets */
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        masking = true
    }

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    TimeCenter.update(
        system = Timekeeper(SystemTimeSource()),
        game = Timekeeper(SystemTimeSource())
    )

    /* 8. Setup ServerContext */
    val dataStore = MongoDataStore(mongoc.getDatabase(Venue.encore.database.dbNameProd), MongoCollectionName)
    val accountRepository = MongoAccountRepository(db.getCollection(MongoCollectionName.playerAccount))
    val contextRegistry = ContextRegistry(RealContextFactory(dataStore))
    val playerLifecycleHandler = PlayerLifecycleHandler()
    val fanchantGuideRegistry = FanchantGuideRegistry()
    val stageActDirector = StageActDirector(TimeCenter.system, ActIdStore)
    val commandDispatcher = CommandDispatcher()
    val webSocketManager = WebSocketManager()

    val accountSubunit = AccountSubunit(accountRepository)
    val playerPresenceSubunit = PlayerPresenceSubunit()
    val sessionSubunit = SessionSubunit(appScope, TimeCenter.system)
    val playerCreationSubunit = PlayerCreationSubunit(dataStore)
    val authSubunit = AuthSubunit(accountSubunit, playerCreationSubunit, sessionSubunit)

    val subunits = ServerSubunits(
        account = accountSubunit,
        presence = playerPresenceSubunit,
        auth = authSubunit,
        session = sessionSubunit,
        creation = playerCreationSubunit
    )
    val serverContext = ServerContext(
        dataStore = dataStore,
        contextRegistry = contextRegistry,
        playerLifecycleHandler = playerLifecycleHandler,
        fanchantGuideRegistry = fanchantGuideRegistry,
        stageActDirector = stageActDirector,
        commandDispatcher = commandDispatcher,
        webSocketManager = webSocketManager,
        subunits = subunits
    )

    webSocketManager.registerHandler(WsCommandHandler(serverContext))

    // initialize components with circular dependency
    // possible solution for dependency: pass studiocontext on runtime action
    // add ServerContext to parameter of ws handle or command dispatch handle
    commandDispatcher.initialize(serverContext)

    commandDispatcher.register(ExampleCommand())

    playerCreationSubunit.createAdmin(Globals, false)

    /* 9. Initialize GameDefinition */
    GameReference.initialize {}

    // represent ephemeral token storage generated to enter /backstage
    val backstageToken = ConcurrentHashMap<String, Long>()

    /* 10. Register routes */
    routing {
        fileRoutes()
        with(BackstageRoutes(serverContext, backstageToken)) { install() }
    }

    interceptResponse()

    val bannedAddresses = mutableSetOf<String>()
    val security = DefaultSecurity(bannedAddresses, TimeCenter.system)
    configureSecurity(security)

    /* 11. Initialize servers */
    // build server configs
    val gameStageConfig = GameStageConfig(
        host = Venue.encore.server.host,
        port = Venue.encore.server.socketPort
    )

    val apiPort = Venue.encore.server.port
    Fancam.info(Tags.Startup) { "Server successfully started." }
    Fancam.info(Tags.Startup) { "File/API server available at ${gameStageConfig.host}:$apiPort." }
    Fancam.info(Tags.Startup) { "Devtools available at ${gameStageConfig.host}:$apiPort/backstage." }

    if (File("docs_build/index.html").exists()) {
        Fancam.info(Tags.Startup) { "Docs website available on ${gameStageConfig.host}:$apiPort." }
    } else {
        Fancam.info(Tags.Startup) { "Docs website not available. Optionally, run 'npm install' & 'npm run dev' in the docs folder to preview it." }
    }

    val gameStage = GameStage(gameStageConfig) { socketDispatcher, serverContext ->
        val possibleFormats = listOf<FanchantGuide<*>>()
        possibleFormats.forEach {
            serverContext.fanchantGuideRegistry.register(it)
        }
    }

    val servers = buildList<Stage> {
        add(gameStage)
    }

    servers.forEach { server ->
        server.initialize(appScope, serverContext)
        server.start()
    }

    launch(Dispatchers.IO) {
        while (isActive) {
            val cmd = withContext(Dispatchers.IO) { readlnOrNull() } ?: break
            val clean = cmd.trim().lowercase()
            if (clean.isNotBlank()) {
                when (clean) {
                    "token" -> {
                        val token = Ids.uuid()
                        println(token)
                        backstageToken[token] = TimeCenter.system.now()
                        val toRemove = mutableListOf<String>()
                        backstageToken.forEach { (token, millis) ->
                            if (TimeCenter.system.hasElapsedBy(millis, 1.minutes)) {
                                toRemove.add(token)
                            }
                        }
                        toRemove.forEach {
                            backstageToken.remove(it)
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    println(EncoreIdentity.banner(GameIdentity))

    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            try {
                servers.forEach { server ->
                    server.shutdown()
                }
                appScope.cancel("Application closed")
                appScope.coroutineContext.job.cancel()
            } catch (_: CancellationException) {
            }
        }
        Fancam.info(Tags.Shutdown) { "Server shutdown complete." }
    })
}

fun Application.configureSecurity(security: SecurityGuard) {
    intercept(ApplicationCallPipeline.Plugins) {
        when (val result = security.verify(call)) {
            is GuardResult.Welcome -> proceed()
            is GuardResult.GetOut -> {
                Fancam.debug(Tags.Api) { call.stringifyHttpRequest(unhandled = false) }

                call.respondText(
                    text = errorHtml(403, result.why),
                    contentType = ContentType.Text.Html,
                    status = HttpStatusCode.Forbidden
                )

                val remote = call.request.origin.remoteHost
                Fancam.info(Tags.Api) { "Security refused $remote due to: ${result.why}" }

                finish()
            }
        }
    }
}

/**
 * Serve file-related endpoints.
 *
 * This mostly serving static files:
 * - Game and website assets in the `assets` folder.
 * - Docs website on production in the `docs_build` folder.
 *
 * Since this is simple, it doesn't use the [RouteHandler]
 */
fun Route.fileRoutes() {
    get("/") {
        call.respondFile(File("assets/site/index.html"))
    }
    staticFiles("site", File("assets/site"))

    val docsDir = File("docs_build")
    if (File(docsDir, "index.html").exists()) {
        staticFiles("docs", docsDir)
    } else {
        get("/docs") {
            call.respond(
                HttpStatusCode.NotFound,
                "Docs website not available. Please start it with a separate vite server. " +
                        "If in prod, build the documentation website to access it."
            )
        }
    }
}

/**
 * Simple HTML template of an error page.
 *
 * @param code HTTP response status code.
 * @param message Context message to display.
 */
fun errorHtml(code: Int, message: String): String {
    return "<!doctypehtml>" +
            "<html lang=en>" +
            "<meta charset=UTF-8>" +
            "<title>Uh-oh... contract ended</title>" +
            "<style>" +
            "body{font-family:system-ui,-apple-system,sans-serif;background-color:#f5f5f5}" +
            "h1{font-size:1.2rem}</style>" +
            "<div class=container>" +
            "<h1>Error: $code</h1>" +
            "<p>$message" +
            "</div>"
}
