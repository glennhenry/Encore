package network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import server.core.network.TestConnection
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * To test the [TestConnection] class.
 */
class TestTestConnection {
    @Test
    fun testConnection() = runTest {
        val conn = TestConnection(
            connectionScope = CoroutineScope(StandardTestDispatcher()),
            playerId = "p1",
            playerName = "Alice"
        )
        conn.enqueueIncoming("Hello".toByteArray())

        val (_, bytes) = conn.read() // returns the injected message
        assertEquals("Hello", String(bytes))

        conn.write("World".toByteArray())
        assertEquals("World", String(conn.getOutgoing().first()))
    }
}
