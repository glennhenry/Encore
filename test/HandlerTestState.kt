import context.FakeContextTracker
import context.PlayerContext
import context.PlayerServices
import context.ServerContext
import data.collection.PlayerAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import server.core.network.TestConnection
import server.handler.DefaultHandlerContext
import server.handler.HandlerContext
import server.messaging.socket.SocketMessage

/**
 * Utility to build state to test socket message handlers.
 */
data class HandlerTestState<T : SocketMessage>(
    val playerId: String = "testPlayerId123",
    val playerName: String = "TestPlayerABC",
    val message: T,
    val account: PlayerAccount = PlayerAccount.fake(playerId, playerName),
    val services: PlayerServices,
    val connectionScope: CoroutineScope = CoroutineScope(StandardTestDispatcher())
) {
    val connection = TestConnection(
        connectionScope = connectionScope,
        playerId = playerId,
        playerName = playerName
    )

    val contextTracker = FakeContextTracker()

    val serverContext = ServerContext.fake(contextTracker = contextTracker)

    val playerContext = PlayerContext(playerId, connection, account, services).also {
        contextTracker.fakeContext(it)
    }

    val handlerContext: HandlerContext<T> = DefaultHandlerContext(
        playerId = playerId,
        message = message,
        connection = connection
    )
}
