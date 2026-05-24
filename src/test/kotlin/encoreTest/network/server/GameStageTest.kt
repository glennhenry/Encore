package encoreTest.network.server

import encore.context.ServerContext
import encore.datastore.collection.PlayerId
import encore.network.fanchant.Fanchant
import encore.network.fanchant.FanchantType
import encore.network.fanchant.guide.DecodeResult
import encore.network.fanchant.guide.FanchantGuide
import encore.network.handler.FanchantHandler
import encore.network.handler.HandlerContext
import encore.network.stage.GameStage
import encore.network.transport.ConnectionIdentity
import encore.network.transport.TestConnection
import encore.utils.safeAsciiString
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Integration test of game server components.
 *
 * Given arbitrary byte, the GameStage should decode, dispatch,
 * and handle the message correctly.
 *
 * Uses [GameStage.activateConnection] with [TestConnection] directly instead
 * of making actual socket connection (though the socket port 7777 will still be used).
 */
class GameStageTest {
    /**
     * - multiple formats are registered
     * - multiple handlers with unique fanchantType
     * choose the format, materialize, and dispatched into the appropriate handler
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `success handling with casual packet`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val gameStage = GameStage("127.0.0.1", 7770) {
            guide(Guide1())
            guide(Guide2())
            guide(Guide3())
            handler(Fc1Handler())
            handler(Fc2Handler())
            handler(Fc3Handler())
        }
        gameStage.initialize(scope, ServerContext.createForTest())
        gameStage.start()

        val connection = createConnection(scope = scope)
        gameStage.activateConnection(connection)

        // "a12345" will be identified as Guide1 and materialized into Fc1
        val packet = "a12345".toByteArray()
        connection.enqueueIncoming(packet)

        connection.awaitOutgoing(1)

        // This will be handled by Fc1Handler which returns 1 1 1
        val result = connection.getOutgoing()
        assertEquals(1, result.size)
        assertContentEquals(byteArrayOf(1, 1, 1), result.first())
    }

    /**
     * - multiple formats are registered
     * - multiple handlers with unique fanchantType
     * - however, no format decoded successfully
     * should be taken care by all rounder handler
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handled by all rounder handler when no fanchant guide matches`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val gameStage = GameStage("127.0.0.1", 7771) {
            guide(Guide1())
            guide(Guide2())
            guide(Guide3())
            handler(Fc1Handler())
            handler(Fc2Handler())
            handler(Fc3Handler())
        }
        gameStage.initialize(scope, ServerContext.createForTest())
        gameStage.start()

        val connection = createConnection(scope = scope)
        gameStage.activateConnection(connection)

        // nobody can handle this (decode fails)
        val packet = "wioenyrv😍😂80u803uvr💀".toByteArray()
        connection.enqueueIncoming(packet)

        // can't use awaitOutgoing since it waits until getOutgoing is non empty

        assertTrue(connection.getOutgoing().isEmpty())
    }

    /**
     * .\gradlew test --tests "encoreTest.network.server.GameStageTest.should capable serving multiple clients"
     *
     * No assertation in this, but successful test of this shouldn't produce
     * server fatal error that causes innocent clients to disrupt.
     *
     * 1. 1 client failure does not distrupt other
     * 2. 1 client failure will stop its connection, effectively calling Connection.shutdown
     *    and wouldn't be able to recover without re-connecting (must with different scope)
     */
    @Test
    fun `should capable serving multiple clients`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val gameStage = GameStage("127.0.0.1", 7772) {
            guide(Guide1())
            guide(Guide2())
            guide(Guide3())
            handler(Fc1Handler())
            handler(Fc2Handler())
            handler(Fc3Handler())
            handler(Fc4Handler())
        }
        gameStage.initialize(scope, ServerContext.createForTest())
        gameStage.start()

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
        gameStage.activateConnection(connection1)
        gameStage.activateConnection(connection2A)
        delay(1.seconds)
        gameStage.activateConnection(connection3)

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
        gameStage.activateConnection(connection2B)
        delay(1.seconds)
        connection2B.enqueueIncoming(packetB)
        connection3.enqueueIncoming(packetC)
        delay(1.seconds)

        // client 1 re-enter
        gameStage.activateConnection(connection1)
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
    }

    /**
     * Must use CoroutineScope.backgroundScope
     */
    private fun createConnection(
        playerId: PlayerId = "playerId123",
        username: String = "playerABC",
        scope: CoroutineScope
    ): TestConnection {
        return TestConnection(
            connectionScope = scope,
            identity = ConnectionIdentity(playerId, username, "N/A")
        )
    }
}

class Guide1 : FanchantGuide<String> {
    override fun verify(data: ByteArray): Boolean = true

    override fun tryDecode(data: ByteArray): DecodeResult<String> {
        val decoded = data.safeAsciiString()
        return if (!decoded.contains("a")) {
            DecodeResult.Failure()
        } else {
            DecodeResult.Success(decoded)
        }
    }

    override fun materialize(decoded: String): Fanchant {
        return Fc1(decoded)
    }
}

class Guide2 : FanchantGuide<String> {
    override fun verify(data: ByteArray): Boolean = true

    override fun tryDecode(data: ByteArray): DecodeResult<String> {
        val decoded = data.safeAsciiString()
        return if (!decoded.contains("b")) {
            DecodeResult.Failure()
        } else {
            DecodeResult.Success(decoded)
        }
    }

    override fun materialize(decoded: String): Fanchant {
        return Fc2(decoded)
    }
}

class Guide3 : FanchantGuide<String> {
    override fun verify(data: ByteArray): Boolean = true

    override fun tryDecode(data: ByteArray): DecodeResult<String> {
        val decoded = data.safeAsciiString()
        return if (!decoded.contains("c")) {
            DecodeResult.Failure()
        } else {
            DecodeResult.Success(decoded)
        }
    }

    override fun materialize(decoded: String): Fanchant {
        return Fc3(decoded)
    }
}

class Fc1(val payload: String) : Fanchant {
    override val type: FanchantType<*> = Fc1Type
    override fun toString(): String = "Fc1($payload)"
}

object Fc1Type : FanchantType<Fc1> {
    override val id: String = "type-fc1"
}

class Fc2(val payload: String) : Fanchant {
    override val type: FanchantType<*> = Fc2Type
    override fun toString(): String = "Fc2($payload)"
}

object Fc2Type : FanchantType<Fc2> {
    override val id: String = "type-fc2"
}

class Fc3(val payload: String) : Fanchant {
    override val type: FanchantType<*> = Fc3Type
    override fun toString(): String = "Fc3($payload)"
}

object Fc3Type : FanchantType<Fc3> {
    override val id: String = "type-fc3"
}

class Fc4(val payload: String) : Fanchant {
    override val type: FanchantType<*> = Fc4Type
    override fun toString(): String = "Fc4($payload)"
}

object Fc4Type : FanchantType<Fc4> {
    override val id: String = "type-fc4"
}

class Fc1Handler : FanchantHandler<Fc1> {
    override val fanchantType: FanchantType<Fc1> = Fc1Type
    override suspend fun handle(ctx: HandlerContext<Fc1>) {
        ctx.sendRaw(byteArrayOf(1, 1, 1))
    }
}

class Fc2Handler : FanchantHandler<Fc2> {
    override val fanchantType: FanchantType<Fc2> = Fc2Type
    override suspend fun handle(ctx: HandlerContext<Fc2>) {
        ctx.sendRaw(byteArrayOf(2, 2, 2))
    }
}

class Fc3Handler : FanchantHandler<Fc3> {
    override val fanchantType: FanchantType<Fc3> = Fc3Type
    override suspend fun handle(ctx: HandlerContext<Fc3>) {
        ctx.sendRaw(byteArrayOf(3, 3, 3))
    }
}

class Fc4Handler : FanchantHandler<Fc4> {
    override val fanchantType: FanchantType<Fc4> = Fc4Type
    override suspend fun handle(ctx: HandlerContext<Fc4>) {
        throw Exception("Requested on Handler9")
    }
}
