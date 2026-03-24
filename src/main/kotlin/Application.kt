import com.mongodb.kotlin.client.coroutine.MongoClient
import encore.api.routes.backstageRoutes
import encore.api.routes.fileRoutes
import encore.api.routes.timeUnderMinutes
import encore.context.DefaultContextTracker
import encore.context.ServerContext
import encore.context.ServerServices
import encore.core.data.GameDefinition
import encore.db.MongoImpl
import encore.backstage.command.core.CommandDispatcher
import encore.backstage.command.impl.ExampleCommand
import encore.server.GameServer
import encore.server.GameServerConfig
import encore.server.ServerContainer
import encore.server.core.OnlinePlayerRegistry
import encore.server.core.Server
import encore.server.messaging.format.MessageFormat
import encore.server.messaging.format.MessageFormatRegistry
import encore.server.tasks.ServerTaskDispatcher
import encore.server.tasks.TaskName
import encore.startup.venue.Venue
import encore.user.PlayerAccountRepositoryMongo
import encore.user.auth.DefaultAuthProvider
import encore.user.auth.SessionManager
import encore.serialization.JSON
import encore.utils.UUID
import encore.fancam.Fancam
import encore.fancam.impl.OfficialFancam
import encore.ws.WebSocketManager
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.date.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.bson.Document
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

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
            Fancam.error { "Internal server error: ${call.request.httpMethod} ${call.request.uri}: ${cause.message}" }

            val message = if (this@module.developmentMode) {
                cause.stackTrace.joinToString("\n")
            } else {
                "Stage was sabotaged..."
            }

            call.respondText(
                text = errorHtml(500, "<pre>${message}</pre>"),
                contentType = ContentType.Text.Html,
                status = HttpStatusCode.InternalServerError
            )
        }

        unhandled { call ->
            Fancam.error { "Unhandled API route: ${call.request.httpMethod} ${call.request.uri}." }
            call.respondText(
                text = errorHtml(404, "Not found in the system."),
                contentType = ContentType.Text.Html,
                status = HttpStatusCode.NotFound
            )
        }
    }

    /* 6. Configure Database */
    val database = startMongo(
        databaseName = Venue.encore.database.dbNameProd,
        mongoUrl = Venue.encore.database.dbUrlProd,
        adminEnabled = Venue.encore.adminEnabled
    )

    /* 7. Install websockets */
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        masking = true
    }

    /* 8. Setup ServerContext */
    val playerAccountRepository = PlayerAccountRepositoryMongo(database.getCollection("player_account"))
    val sessionManager = SessionManager()
    val authProvider = DefaultAuthProvider(database, playerAccountRepository, sessionManager)
    val onlinePlayerRegistry = OnlinePlayerRegistry()
    val contextTracker = DefaultContextTracker()
    val codecDispatcher = MessageFormatRegistry()
    val taskDispatcher = ServerTaskDispatcher()
    val commandDispatcher = CommandDispatcher()
    val wsManager = WebSocketManager()
    val services = ServerServices()
    val serverContext = ServerContext(
        db = database,
        playerAccountRepository = playerAccountRepository,
        sessionManager = sessionManager,                   // is not used unless auth is implemented
        authProvider = authProvider,                       // is not used unless auth is implemented
        onlinePlayerRegistry = onlinePlayerRegistry,       // not much used typically
        contextTracker = contextTracker,
        formatRegistry = codecDispatcher,
        taskDispatcher = taskDispatcher,
        commandDispatcher = commandDispatcher,
        wsManager = wsManager,
        services = services
    )

    // initialize components with circular dependency
    wsManager.initialize(serverContext)
    commandDispatcher.initialize(serverContext)

    commandDispatcher.register(ExampleCommand())

    /* 9. Initialize GameDefinition */
    GameDefinition.initialize()

    // represent ephemeral token storage generated to enter /backstage
    val backstageToken = ConcurrentHashMap<String, Long>()

    /* 10. Register routes */
    routing {
        fileRoutes()
        backstageRoutes(serverContext, backstageToken)
    }

    /* 11. Initialize servers */
    // build server configs
    val gameServerConfig = GameServerConfig(
        host = Venue.encore.server.host,
        port = Venue.encore.server.socketPort
    )

    val apiPort = Venue.encore.server.port
    Fancam.info { "Server successfully started." }
    Fancam.info { "File/API server available at ${gameServerConfig.host}:$apiPort." }
    Fancam.info { "Devtools available at ${gameServerConfig.host}:$apiPort/backstage." }

    if (File("docs/index.html").exists()) {
        Fancam.info { "Docs website available on ${gameServerConfig.host}:$apiPort." }
    } else {
        Fancam.trace { "Docs website not available. Optionally, run 'npm install' & 'npm run dev' in the docs folder to preview it." }
    }

    val gameServer = GameServer(gameServerConfig) { socketDispatcher, serverContext ->
        // REPLACE
        serverContext.taskDispatcher.registerTask(
            name = TaskName.DummyName,
            stopFactory = {},
            deriveTaskId = { playerId, name, _ ->
                // RTD-playerId123-unit
                "${name.code}-$playerId-unit"
            }
        )
        // REPLACE ADD
        val possibleFormats = listOf<MessageFormat<*>>(

        )
        possibleFormats.forEach {
            serverContext.formatRegistry.register(it)
        }
    }

    val servers = buildList<Server> {
        add(gameServer)
    }

    /* 12. Run all the servers */
    val container = ServerContainer(servers, serverContext)
    run {
        container.initializeAll()
        container.startAll()
    }

    launch(Dispatchers.IO) {
        while (isActive) {
            val cmd = withContext(Dispatchers.IO) { readlnOrNull() } ?: break
            val clean = cmd.trim().lowercase()
            if (clean.isNotBlank()) {
                when (clean) {
                    "token" -> {
                        val token = UUID.new()
                        println(token)
                        backstageToken[token] = getTimeMillis()
                        val toRemove = mutableListOf<String>()
                        backstageToken.forEach { (token, millis) ->
                            if (!timeUnderMinutes(millis, 1)) {
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

    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            container.shutdownAll()
        }
        Fancam.info { "Server shutdown complete." }
    })
}

fun startMongo(databaseName: String, mongoUrl: String, adminEnabled: Boolean): MongoImpl {
    return runBlocking {
        try {
            val mongoc = MongoClient.create(mongoUrl)
            val db = mongoc.getDatabase("admin")
            val commandResult = db.runCommand(Document("ping", 1))
            Fancam.info { "MongoDB connection successful: $commandResult" }
            MongoImpl(mongoc.getDatabase(databaseName), adminEnabled)
        } catch (e: Exception) {
            Fancam.error { "MongoDB connection failed inside timeout: ${e.message}" }
            throw e
        }
    }
}

/**
 * HTML template for error page.
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
