package testHelper

import encore.context.FakeContextTracker
import encore.context.PlayerContext
import encore.context.PlayerSubunits
import encore.context.ServerContext
import encore.datastore.collection.PlayerAccount
import encore.network.transport.TestConnection
import encore.network.handler.DefaultHandlerContext
import encore.network.handler.HandlerContext
import encore.network.messaging.socket.SocketMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher

/**
 * Handler testing utility to encapsulate the relevant states to test message handlers.
 */
data class HandlerTestState<T : SocketMessage>(
    val playerId: String = "testPlayerId123",
    val playerName: String = "TestPlayerABC",
    val message: T,
    val account: PlayerAccount = createAccount(playerId, playerName, "anypassword"),
    val subunits: PlayerSubunits,
    val connectionScope: CoroutineScope = CoroutineScope(StandardTestDispatcher())
) {
    val connection = TestConnection(
        connectionScope = connectionScope,
        playerId = playerId,
        playerName = playerName
    )

    val contextTracker = FakeContextTracker()

    val serverContext = ServerContext.createForTest(contextTracker = contextTracker)

    val playerContext = PlayerContext(playerId, connection, account, subunits).also {
        contextTracker.fakeContext(it)
    }

    val handlerContext: HandlerContext<T> = DefaultHandlerContext(
        playerId = playerId,
        message = message,
        connection = connection
    )
}
