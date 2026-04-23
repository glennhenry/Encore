package encoreTest.network.server

import encore.annotation.source.RevisitLater
import encore.context.ServerContext
import encore.network.server.GameServer
import encore.network.server.GameServerConfig
import encore.network.server.ServerContainer
import encore.network.transport.TestConnection
import encore.network.handler.HandlerContext
import encore.network.handler.SocketMessageHandler
import encore.network.messaging.format.DecodeResult
import encore.network.messaging.format.MessageFormat
import encore.network.messaging.socket.SocketMessage
import encore.utils.safeAsciiString
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.reflect.KClass
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

/**
 * Integration test of game server components.
 *
 * Given arbitrary byte, the GameServer should decode, dispatch,
 * and handle the message correctly.
 *
 * Uses [GameServer.handleClient] with [TestConnection] directly instead
 * of making actual socket connection (though the socket port 7777 will still be used).
 */
class GameServerTest {
    private val config = GameServerConfig(host = "127.0.0.1", port = 7777)

    /**
     * - multiple formats
     * - multiple handlers
     * - multiple expected type
     * - multiple expected message class
     * - one success decode
     * - two expected handler
     * - dispatched and handled correctly.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `success handling with casual packet`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val gameServer = GameServer(config) { socketDispatcher, serverContext ->
            val possibleFormats = listOf<MessageFormat<*>>(
                ExFormat3(), ExFormat4(), ExFormat5()
            )
            possibleFormats.forEach {
                serverContext.messageFormatRegistry.register(it)
            }
            socketDispatcher.register(Handler5())
            socketDispatcher.register(Handler6())
            socketDispatcher.register(Handler7())
            socketDispatcher.register(Handler6())
        }
        val container = ServerContainer(scope, listOf(gameServer), ServerContext.createForTest())
        container.initializeAll()
        container.startAll()

        val connection = createConnection(scope = scope)
        gameServer.handleClient(connection)
        // ExFormat3 ExMsg3 type1 handled by Handler5
        // must have 'a' to be considered as ExFormat3
        val packet = "a12345".toByteArray()
        connection.enqueueIncoming(packet)

        connection.awaitOutgoing(1)

        // Handler5 returns 5 5 5
        val result = connection.getOutgoing()
        assertEquals(1, result.size)
        assertContentEquals(byteArrayOf(5, 5, 5), result.first())
        container.shutdownAll()
    }

    /**
     * - multiple handlers
     * - multiple expected type
     * - multiple expected message class
     * - all decode fails (junk message)
     */
    @Test
    @RevisitLater("Test failed")
    fun `failed handling with junk packet`() = runTest {
        val gameServer = GameServer(config) { socketDispatcher, serverContext ->
            val possibleFormats = listOf<MessageFormat<*>>(
                ExFormat3(), ExFormat4(), ExFormat5()
            )
            possibleFormats.forEach {
                serverContext.messageFormatRegistry.register(it)
            }
            socketDispatcher.register(Handler5())
            socketDispatcher.register(Handler6())
            socketDispatcher.register(Handler7())
            socketDispatcher.register(Handler6())
        }
        val container = ServerContainer(this, listOf(gameServer), ServerContext.createForTest())
        container.initializeAll()
        container.startAll()

        val connection = createConnection(scope = this.backgroundScope)
        gameServer.handleClient(connection)
        // nobody can handle this (decode fails)
        val packet = "wioenyrv😍😂80u803uvr💀".toByteArray()
        connection.enqueueIncoming(packet)

        // can't use awaitOutgoing since it waits until getOutgoing is non empty

        assertTrue(connection.getOutgoing().isEmpty())
        container.shutdownAll()
    }

    /**
     * - multiple handlers
     * - multiple expected type
     * - multiple expected message class
     * - multiple decode succeed (warned)
     * - dispatched and handled correctly.
     */
    @Test
    fun `success handling with casual packet, but warned`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val gameServer = GameServer(config) { socketDispatcher, serverContext ->
            val possibleFormats = listOf<MessageFormat<*>>(
                ExFormat3(), ExFormat4(), ExFormat5(), ExFormat6()
            )
            possibleFormats.forEach {
                serverContext.messageFormatRegistry.register(it)
            }
            socketDispatcher.register(Handler5())
            socketDispatcher.register(Handler6())
            socketDispatcher.register(Handler7())
            socketDispatcher.register(Handler8())
        }
        val container = ServerContainer(scope, listOf(gameServer), ServerContext.createForTest())
        container.initializeAll()
        container.startAll()

        val connection = createConnection(scope = scope)
        gameServer.handleClient(connection)
        // ExFormat5 and ExFormat6 has same decoding process
        // but decode to ExMsg5 and ExMsg4 respectively
        val packet = "c12345".toByteArray()
        connection.enqueueIncoming(packet)

        connection.awaitOutgoing(1)

        // ExFormat5 will be chosen, based on registration order
        // Handler6 handles ExMsg5, returning 6 6 6
        val result = connection.getOutgoing()
        assertEquals(1, result.size)
        assertContentEquals(byteArrayOf(6, 6, 6), result.first())
        // not asserted but you should see warning in logger

        container.shutdownAll()
    }

    @Test
    @Ignore("slow timer 11 seconds")
    @RevisitLater(
        """
        1. Decouple server start, accept, and handle so it's easy to add client without calling handleClient directly.
        2. Decouple player connection lifecycle. 
        3. Find way to assert active clients.
        """
    )
    /**
     * No assertation in this, but successful test of this shouldn't produce
     * server fatal error that causes innocent clients to disrupt.
     *
     * 1. 1 client failure does not distrupt other
     * 2. 1 client failure will stop its connection, effectively calling Connection.shutdown
     *    and wouldn't be able to recover without re-connecting (must with different scope)
     */
    fun `should capable serving multiple clients`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val gameServer = GameServer(config) { socketDispatcher, serverContext ->
            val possibleFormats = listOf<MessageFormat<*>>(
                ExFormat7(), ExFormat3(), ExFormat4(), ExFormat5(),
            )
            possibleFormats.forEach {
                serverContext.messageFormatRegistry.register(it)
            }
            socketDispatcher.register(Handler5())
            socketDispatcher.register(Handler6())
            socketDispatcher.register(Handler7())
            socketDispatcher.register(Handler8())
            socketDispatcher.register(Handler9())
        }
        val context = ServerContext.createForTest()
        val container = ServerContainer(scope, listOf(gameServer), context)
        container.initializeAll()
        container.startAll()

        // preparation
        val packetA = "a12345".toByteArray()
        val packetB = "bxxxxx".toByteArray()
        val packetC = "xxxxxc".toByteArray()
        val packetError = "666".toByteArray()

        val connection1 = createConnection("c1Id", "c1Name", CoroutineScope(SupervisorJob() + Dispatchers.Default))
        val connection2A = createConnection("c2Id", "c2Name", CoroutineScope(SupervisorJob() + Dispatchers.Default))
        val connection2B = createConnection("c2Id", "c2Name", CoroutineScope(SupervisorJob() + Dispatchers.Default))
        val connection3 = createConnection("c3Id", "c3Name", CoroutineScope(SupervisorJob() + Dispatchers.Default))

        delay(1.seconds)

        // clients connect
        gameServer.handleClient(connection1)
        gameServer.handleClient(connection2A)
        delay(1.seconds)
        gameServer.handleClient(connection3)

        // clients send message (1)
        connection1.enqueueIncoming(packetA)
        delay(1.seconds)
        connection2A.enqueueIncoming(packetError)
        connection3.enqueueIncoming(packetA)
        delay(1.seconds)

        // client 1 shutdown
        connection1.shutdown()
        delay(1.seconds)

        // clients send message (2)
        // client 2 must reconnect (+ different scope) because it fails before
        gameServer.handleClient(connection2B)
        delay(1.seconds)
        connection2B.enqueueIncoming(packetB)
        connection3.enqueueIncoming(packetC)
        delay(1.seconds)

        // client 1 re-enter
        gameServer.handleClient(connection1)
        delay(1.seconds)

        // clients send message (3) with handler failure on client 3
        connection3.enqueueIncoming(packetError)
        connection1.enqueueIncoming(packetA)
        connection2B.enqueueIncoming(packetC)
        delay(2.seconds)

        // all clients shutdown
        connection1.shutdown()
        connection2B.shutdown()
        delay(1.seconds)
        // client 3 is already shut down, calling shutdown here is not needed

        // server shutdown
        container.shutdownAll()
    }

    /**
     * Must use CoroutineScope.backgroundScope
     */
    private fun createConnection(
        playerId: String = "playerId123",
        username: String = "playerABC",
        scope: CoroutineScope
    ): TestConnection {
        return TestConnection(
            connectionScope = scope,
            playerId = playerId,
            playerName = username
        )
    }
}

class ExFormat3 : MessageFormat<String> {
    override val name: String = "ExFormat3"
    override fun verify(data: ByteArray): Boolean = true

    override fun tryDecode(data: ByteArray): DecodeResult<String> {
        val decoded = data.safeAsciiString()
        return if (!decoded.contains("a")) {
            DecodeResult.Failure()
        } else {
            DecodeResult.Success(decoded)
        }
    }

    override fun materialize(decoded: String): SocketMessage {
        return ExMsg3(decoded)
    }
}

class ExFormat4 : MessageFormat<String> {
    override val name: String = "ExFormat4"
    override fun verify(data: ByteArray): Boolean = true

    override fun tryDecode(data: ByteArray): DecodeResult<String> {
        val decoded = data.safeAsciiString()
        return if (!decoded.contains("b")) {
            DecodeResult.Failure()
        } else {
            DecodeResult.Success(decoded)
        }
    }

    override fun materialize(decoded: String): SocketMessage {
        return ExMsg4(decoded)
    }
}

class ExFormat5 : MessageFormat<String> {
    override val name: String = "ExFormat5"
    override fun verify(data: ByteArray): Boolean = true

    override fun tryDecode(data: ByteArray): DecodeResult<String> {
        val decoded = data.safeAsciiString()
        return if (!decoded.contains("c")) {
            DecodeResult.Failure()
        } else {
            DecodeResult.Success(decoded)
        }
    }

    override fun materialize(decoded: String): SocketMessage {
        return ExMsg5(decoded)
    }
}

class ExFormat6 : MessageFormat<String> {
    override val name: String = "ExFormat6"
    override fun verify(data: ByteArray): Boolean = true

    override fun tryDecode(data: ByteArray): DecodeResult<String> {
        val decoded = data.safeAsciiString()
        return if (!decoded.contains("c")) {
            DecodeResult.Failure()
        } else {
            DecodeResult.Success(decoded)
        }
    }

    override fun materialize(decoded: String): SocketMessage {
        return ExMsg4(decoded)
    }
}

class ExFormat7 : MessageFormat<String> {
    override val name: String = "ExFormat7"
    override fun verify(data: ByteArray): Boolean = true

    override fun tryDecode(data: ByteArray): DecodeResult<String> {
        if (data.safeAsciiString().startsWith("666")) {
            return DecodeResult.Success("666 error request to Handler9")
        }
        return DecodeResult.Failure()
    }

    override fun materialize(decoded: String): SocketMessage {
        return ExMsg6(decoded)
    }
}

class ExMsg3(val payload: String) : SocketMessage {
    override fun type(): String = "type1"
    override fun toString(): String = "ExMsg3($payload)"
}

class ExMsg4(val payload: String) : SocketMessage {
    override fun type(): String = "type1"
    override fun toString(): String = "ExMsg4($payload)"
}

class ExMsg5(val payload: String) : SocketMessage {
    override fun type(): String = "type2"
    override fun toString(): String = "ExMsg5($payload)"
}

class ExMsg6(val payload: String) : SocketMessage {
    override fun type(): String = "type6"
    override fun toString(): String = "ExMsg6($payload)"
}

class Handler5 : SocketMessageHandler<ExMsg3> {
    override val name: String = "Handler5"
    override val messageType: String = "type1"
    override val expectedMessageClass: KClass<ExMsg3> = ExMsg3::class

    override suspend fun handle(ctx: HandlerContext<ExMsg3>) {
        ctx.sendRaw(byteArrayOf(5, 5, 5))
    }
}

class Handler6 : SocketMessageHandler<ExMsg5> {
    override val name: String = "Handler6"
    override val messageType: String = "type2"
    override val expectedMessageClass: KClass<ExMsg5> = ExMsg5::class

    override suspend fun handle(ctx: HandlerContext<ExMsg5>) {
        ctx.sendRaw(byteArrayOf(6, 6, 6))
    }
}

class Handler7 : SocketMessageHandler<ExMsg4> {
    override val name: String = "Handler7"
    override val messageType: String = "type3"
    override val expectedMessageClass: KClass<ExMsg4> = ExMsg4::class

    override suspend fun handle(ctx: HandlerContext<ExMsg4>) {
        ctx.sendRaw(byteArrayOf(7, 7, 7))
    }
}

class Handler8 : SocketMessageHandler<ExMsg5> {
    override val name: String = "Handler8"
    override val messageType: String = "type4"
    override val expectedMessageClass: KClass<ExMsg5> = ExMsg5::class

    override suspend fun handle(ctx: HandlerContext<ExMsg5>) {
        ctx.sendRaw(byteArrayOf(8, 8, 8))
    }
}

class Handler9 : SocketMessageHandler<ExMsg6> {
    override val name: String = "Handler9"
    override val messageType: String = "type6"
    override val expectedMessageClass: KClass<ExMsg6> = ExMsg6::class

    override suspend fun handle(ctx: HandlerContext<ExMsg6>) {
        throw Exception("Requested on Handler9")
    }
}
