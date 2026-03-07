package server

import context.ServerContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import server.core.network.TestConnection
import server.handler.HandlerContext
import server.handler.SocketMessageHandler
import server.messaging.format.DecodeResult
import server.messaging.format.MessageFormat
import server.messaging.socket.SocketMessage
import utils.functions.safeAsciiString
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
        val gameServer = GameServer(config) { socketDispatcher, serverContext ->
            val possibleFormats = listOf<MessageFormat<*>>(
                ExFormat3(), ExFormat4(), ExFormat5()
            )
            possibleFormats.forEach {
                serverContext.formatRegistry.register(it)
            }
            socketDispatcher.register(Handler5())
            socketDispatcher.register(Handler6())
            socketDispatcher.register(Handler7())
            socketDispatcher.register(Handler6())
        }
        val container = ServerContainer(listOf(gameServer), ServerContext.fake())
        container.initializeAll()
        container.startAll()

        val connection = createConnection(this.backgroundScope)
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
    }

    /**
     * - multiple handlers
     * - multiple expected type
     * - multiple expected message class
     * - all decode fails (junk message)
     */
    @Test
    @Ignore("slow test, real timer 5 seconds")
    fun `failed handling with junk packet`() = runTest {
        val gameServer = GameServer(config) { socketDispatcher, serverContext ->
            val possibleFormats = listOf<MessageFormat<*>>(
                ExFormat3(), ExFormat4(), ExFormat5()
            )
            possibleFormats.forEach {
                serverContext.formatRegistry.register(it)
            }
            socketDispatcher.register(Handler5())
            socketDispatcher.register(Handler6())
            socketDispatcher.register(Handler7())
            socketDispatcher.register(Handler6())
        }
        val container = ServerContainer(listOf(gameServer), ServerContext.fake())
        container.initializeAll()
        container.startAll()

        val connection = createConnection(this.backgroundScope)
        gameServer.handleClient(connection)
        // nobody can handle this (decode fails)
        val packet = "awioenyrv😍😂80au803uvr💀".toByteArray()
        connection.enqueueIncoming(packet)

        // can't use awaitOutgoing since it waits until getOutgoing is non empty
        delay(5.seconds)

        assertTrue(connection.getOutgoing().isEmpty())
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
        val gameServer = GameServer(config) { socketDispatcher, serverContext ->
            val possibleFormats = listOf<MessageFormat<*>>(
                ExFormat3(), ExFormat4(), ExFormat5(), ExFormat6()
            )
            possibleFormats.forEach {
                serverContext.formatRegistry.register(it)
            }
            socketDispatcher.register(Handler5())
            socketDispatcher.register(Handler6())
            socketDispatcher.register(Handler7())
            socketDispatcher.register(Handler8())
        }
        val container = ServerContainer(listOf(gameServer), ServerContext.fake())
        container.initializeAll()
        container.startAll()

        val connection = createConnection(this.backgroundScope)
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
    }

    /**
     * Must use CoroutineScope.backgroundScope
     */
    private fun createConnection(scope: CoroutineScope): TestConnection {
        return TestConnection(
            connectionScope = scope,
            playerId = "pid123",
            playerName = "PlayerABC"
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
