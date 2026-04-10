package example

import encore.context.PlayerSubunits
import encore.context.ServerContext
import encore.datastore.collection.PlayerAccount
import encore.server.core.network.TestConnection
import encore.server.handler.HandlerContext
import encore.server.handler.SocketMessageHandler
import encore.server.messaging.socket.SocketMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import testHelper.HandlerTestState
import testHelper.createAccount
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Demonstrates how to test a socket message handler.
 *
 * Why test handler?
 * - Ensure handler still works correctly (e.g., may test handler by a recorded packets).
 * - Ensure handler still work on weird condition (e.g., valid but weird payload).
 * - Integration test with subunit classes.
 * - Testing conditions that are hard to achieve realistically. Handler test harness
 *   the power to modify server context, subunits, and other components.
 *
 * This test setup relies on [TestConnection] to inject arbitrary incoming messages
 * and to inspect the outgoing messages produced by handlers.
 *
 * Typically, tests manipulate the server and player state — including [ServerContext],
 * [PlayerAccount], and [PlayerSubunits]. Some fields or components can remain default or empty
 * if they are not relevant to the current test scenario.
 *
 * For classes like subunits that depend on repository interfaces, initialize concrete
 * subunit with fake repository implementations that has predefined data and operations.
 * This allows isolated and flexible testing without requiring a live database or real dependencies.
 *
 * When validating the test results, you must manually deserialize the messages sent by handlers.
 * This is because handlers typically perform their own serialization. You may alternatively
 * serialize the expected value, but both approaches assume that the serializer and deserializer
 * behave correctly.
 */
class ExampleHandlerTest {
    @Test
    fun testHandler1() = runTest {
        val playerId = "pid123"
        val playerName = "player123"

        val state = HandlerTestState(
            playerId = playerId,
            playerName = playerName,
            message = DotSeparatedMessage(payload = "MSG1.EX.hello.world.kotlin.ktor"),
            account = createAccount(playerId, playerName, "anypassword"),
            subunits = PlayerSubunits(),
            connectionScope = CoroutineScope(StandardTestDispatcher())
        )

        val handler = DotSeparatedHandler()
        handler.handle(state.handlerContext)

        val expected = "RESPONSE.EX.HELLOWORLDKOTLINKTOR"
        val bytesSend = state.connection.getOutgoing()
        val deserialized = bytesSend.first().decodeToString()
        assertEquals(expected, deserialized)
    }

    @Test
    fun testHandler2() = runTest {
        val playerId = "pid123"
        val playerName = "player123"

        val state = HandlerTestState(
            playerId = playerId,
            playerName = playerName,
            message = DotSeparatedMessage(payload = "MSG1.EX.hello.world.kotlin|ktor"),
            account = createAccount(playerId, playerName, "anypassword"),
            subunits = PlayerSubunits(),
            connectionScope = CoroutineScope(StandardTestDispatcher())
        )

        val handler = DotSeparatedHandler()
        handler.handle(state.handlerContext)

        val expected = "RESPONSE.EX.FAIL"
        val bytesSend = state.connection.getOutgoing()
        val deserialized = bytesSend.first().decodeToString()
        assertEquals(expected, deserialized)
    }
}

/**
 * Example of handler that handles [DotSeparatedMessage]
 */
class DotSeparatedHandler: SocketMessageHandler<DotSeparatedMessage> {
    override val name: String = "ExampleHandler"
    override val messageType: String = "EX"
    override val expectedMessageClass = DotSeparatedMessage::class

    /**
     * `with(ctx)` gives developer QoL to access `connection` and `message` simpler.
     */
    override suspend fun handle(ctx: HandlerContext<DotSeparatedMessage>) = with(ctx) {
        if (message.payload.contains("|")) {
            val messageToSend = "RESPONSE.EX.FAIL"
            sendRaw(messageToSend.toByteArray())
        } else {
            val cleanPayload = message.payload.substringAfter("MSG1.EX.").split(".")
            val result = StringBuilder()

            cleanPayload.forEach { msg ->
                result.append(msg.uppercase())
            }

            val messageToSend = "RESPONSE.EX.$result"
            sendRaw(messageToSend.toByteArray())
        }
    }
}

/**
 * Example of socket message where payload is a String delimited by `.`.
 *
 * In real scenario, the [payload] may be a `List<String>` combined
 * with a message format implementation. It's omitted for simplicity.
 *
 * - First word is message version.
 * - Second word is message type.
 * - The rest are payload.
 */
class DotSeparatedMessage(val payload: String) : SocketMessage {
    override fun type(): String = "EX"
    override fun toString(): String = payload
}
