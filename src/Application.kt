
import api.routes.devtoolsRoutes
import api.routes.fileRoutes
import api.routes.timeUnderMinutes
import com.mongodb.kotlin.client.coroutine.MongoClient
import context.DefaultContextTracker
import context.ServerContext
import context.ServerServices
import core.data.GameDefinition
import data.MongoImpl
import devtools.command.core.CommandDispatcher
import devtools.command.impl.ExampleCommand
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.bson.Document
import server.GameServer
import server.GameServerConfig
import server.ServerContainer
import server.core.OnlinePlayerRegistry
import server.core.Server
import server.messaging.format.MessageFormat
import server.messaging.format.MessageFormatRegistry
import server.tasks.ServerTaskDispatcher
import server.tasks.TaskName
import user.PlayerAccountRepositoryMongo
import user.auth.DefaultAuthProvider
import user.auth.SessionManager
import utils.JSON
import utils.functions.UUID
import utils.logging.Logger
import utils.logging.LoggerSettings
import utils.logging.toInt
import utils.logging.toLogLevel
import ws.WebSocketManager
import java.io.File
import java.text.SimpleDateFormat
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
    EngineMain.main(args)
}

const val CHANGE_ME_PROD_DB_NAME = "CHANGE_ME-prod-DB"
const val SERVER_ADDRESS = "127.0.0.1"
const val SERVER_API_FILE_PORT = 8080
const val SERVER_SOCKET_PORT = 7777

@Suppress("unused")
suspend fun Application.module() {
    /* 1. Setup logger */
    Logger.updateSettings { original ->
        LoggerSettings(
            minimumLevel = config().getInt("logger.level", original.minimumLevel.toInt()).toLogLevel(),
            colorfulLog = config().getBoolean("logger.colorfulLog", original.colorfulLog),
            colorizeLevelLabelOnly = config().getBoolean(
                "logger.colorizeLevelLabelOnly",
                original.colorizeLevelLabelOnly
            ),
            useForegroundColor = config().getBoolean("logger.useForegroundColor", original.useForegroundColor),
            fileNamePadding = config().getInt("logger.maximumFileNameLength", original.fileNamePadding),
            tagPadding = config().getInt("logger.tagPadding", original.tagPadding),
            maximumLogMessageLineLength = config().getInt(
                "logger.maximumLogMessageLength",
                original.maximumLogMessageLineLength
            ),
            maximumLogFileSize = config().getInt("logger.maximumLogFileSize", original.maximumLogFileSize),
            maximumLogFileRotation = config().getInt("logger.maximumLogFileRotation", original.maximumLogFileRotation),
            logDateFormatter = SimpleDateFormat(
                config().getString(
                    "logger.logDateFormatter",
                    original.logDateFormatter.toPattern()
                )
            ),
            fileDateFormatter = SimpleDateFormat(
                config().getString(
                    "logger.fileDateFormatter",
                    original.fileDateFormatter.toPattern()
                )
            )
        )
    }

    /* 2. Setup serialization */
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

    /* 4. Install call logging */
    install(CallLogging)

    /* 5. Install status pages */
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            Logger.error { "Server error at ${call.request.httpMethod} ${call.request.uri}: ${cause.message}" }
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
        unhandled { call ->
            Logger.error { "Unhandled API route: ${call.request.httpMethod} ${call.request.uri}." }
        }
    }

    /* 6. Configure Database */
    val database = startMongo(
        databaseName = CHANGE_ME_PROD_DB_NAME,
        mongoUrl = config().getString("mongo.url", "mongodb://localhost:27017"),
        adminEnabled = config().getBoolean("game.enableAdmin", true)
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
    val commandDispatcher = CommandDispatcher(Logger)
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
    wsManager.init(serverContext)
    commandDispatcher.init(serverContext)

    commandDispatcher.register(ExampleCommand())

    /* 9. Initialize GameDefinition */
    GameDefinition.initialize()

    // represent ephemeral token storage generated to enter /devtools
    val devtoolsToken = mutableMapOf<String, Long>()

    /* 10. Register routes */
    routing {
        fileRoutes()
        devtoolsRoutes(serverContext, devtoolsToken)
    }

    /* 11. Initialize servers */
    // build server configs
    val gameServerConfig = GameServerConfig(
        host = config().getString("game.host", SERVER_ADDRESS),
        port = config().getInt("game.host", SERVER_SOCKET_PORT)
    )

    val apiPort = config().getString("ktor.deployment.port", SERVER_API_FILE_PORT.toString())
    Logger.info { "Server successfully started." }
    Logger.info { "File/API server available at ${gameServerConfig.host}:$apiPort." }
    Logger.info { "Devtools available at ${gameServerConfig.host}:$apiPort/devtools." }

    if (File("docs/index.html").exists()) {
        Logger.info { "Docs website available on ${gameServerConfig.host}:$apiPort." }
    } else {
        Logger.verbose { "Docs website not available. Optionally, run 'npm install' & 'npm run dev' in the docs folder to preview it." }
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
        container.startAcceptingCommandInputs {
            while (isActive) {
                val cmd = withContext(Dispatchers.IO) { readlnOrNull() } ?: break
                val clean = cmd.trim().lowercase()
                if (clean.isNotBlank()) {
                    when (clean) {
                        "token" -> {
                            val token = UUID.new()
                            println(token)
                            devtoolsToken[token] = getTimeMillis()
                            devtoolsToken.map { (token, millis) ->
                                if (!timeUnderMinutes(millis, 1)) {
                                    devtoolsToken.remove(token)
                                }
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            container.shutdownAll()
        }
        Logger.info { "Server shutdown complete." }
    })
}

fun startMongo(databaseName: String, mongoUrl: String, adminEnabled: Boolean): MongoImpl {
    return runBlocking {
        try {
            val mongoc = MongoClient.create(mongoUrl)
            val db = mongoc.getDatabase("admin")
            val commandResult = db.runCommand(Document("ping", 1))
            Logger.info { "MongoDB connection successful: $commandResult" }
            MongoImpl(mongoc.getDatabase(databaseName), adminEnabled)
        } catch (e: Exception) {
            Logger.error { "MongoDB connection failed inside timeout: ${e.message}" }
            throw e
        }
    }
}

// REPLACE add
data class ServerConfig(
    val adminEnabled: Boolean,
    val mongoUrl: String,
)

fun ApplicationConfig.getString(path: String, default: String): String {
    return this.propertyOrNull(path)?.getString() ?: default
}

fun ApplicationConfig.getInt(path: String, default: Int): Int {
    return this.propertyOrNull(path)?.getString()?.toIntOrNull() ?: default
}

fun ApplicationConfig.getBoolean(path: String, default: Boolean): Boolean {
    return this.propertyOrNull(path)?.getString()?.toBooleanStrict() ?: default
}

/**
 * Use the application.yaml config
 */
fun Application.config(): ApplicationConfig = this.environment.config
