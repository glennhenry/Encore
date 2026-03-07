package example

import HandlerTestState
import context.PlayerServices
import context.ServerContext
import data.collection.PlayerAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import server.core.network.TestConnection
import server.handler.HandlerContext
import server.messaging.socket.SocketMessage
import server.handler.SocketMessageHandler
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Demonstrates how to test a socket message handler.
 *
 * This test setup relies on [TestConnection] to inject arbitrary incoming messages
 * and to inspect the outgoing messages produced by handlers.
 *
 * Typically, tests manipulate the server and player state — including [ServerContext],
 * [PlayerAccount], and [PlayerServices]. Some fields or components can remain default or empty
 * if they are not relevant to the current test scenario.
 *
 * For classes like services that depend on repository interfaces, provide fake repository
 * implementations with predefined data and successful operations. This allows isolated and flexible
 * testing without requiring a live database or real dependencies.
 *
 * When validating the test results, you must manually deserialize the messages sent by handlers.
 * This is because handlers typically perform their own serialization. You may alternatively
 * serialize the expected value, but both approaches assume that the serializer and deserializer
 * behave correctly.
 *
 * Things that aren't demonstrated here:
 * - Real service class; although there exist test of service and repo.
 * - ServerContext or services alteration; it's possible to check the modified
 * server context state if the purpose of testing is not only checking the response message.
 */
class ExampleHandlerTest {
    @Test
    fun testHandler1() = runTest {
        val playerId = "pid123"
        val playerName = "player123"

        // encapsulate every state
        val state = HandlerTestState(
            playerId = playerId,
            playerName = playerName,
            message = ExampleMessage2(payload = "MSG1.EX.hello.world.kotlin.ktor"),
            account = PlayerAccount.fake(playerId, playerName),
            services = PlayerServices(),
            connectionScope = CoroutineScope(StandardTestDispatcher())
        )

        val handler = ExampleHandler(state.serverContext)
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

        // encapsulate every state
        val state = HandlerTestState(
            playerId = playerId,
            playerName = playerName,
            message = ExampleMessage2(payload = "MSG1.EX.hello.world.kotlin|ktor"),
            account = PlayerAccount.fake(playerId, playerName),
            services = PlayerServices(),
            connectionScope = CoroutineScope(StandardTestDispatcher())
        )

        val handler = ExampleHandler(state.serverContext)
        handler.handle(state.handlerContext)

        val expected = "RESPONSE.EX.FAIL"
        val bytesSend = state.connection.getOutgoing()
        val deserialized = bytesSend.first().decodeToString()
        assertEquals(expected, deserialized)
    }
}

/**
 * Example of handler that handles ExampleMessage2<String>
 */
class ExampleHandler(private val serverContext: ServerContext) : SocketMessageHandler<ExampleMessage2> {
    override val name: String = "ExampleHandler"
    override val messageType: String = ""
    override val expectedMessageClass = ExampleMessage2::class

    /**
     * `with(ctx)` gives developer QoL to access `connection` and `message` simpler.
     */
    override suspend fun handle(ctx: HandlerContext<ExampleMessage2>) = with(ctx) {
        // example rejection
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
 * In real scenario, payload may be a `List<String>` instead, so handler don't
 * need to handle the delimiter.
 *
 * - First word is message version.
 * - Second word is message type.
 * - The rest are payload.
 */
class ExampleMessage2(val payload: String) : SocketMessage {
    override fun type(): String = "EX"
    override fun toString(): String = payload
}
