package encoreTest.network.server

import encore.network.handler.HandlerContext
import encore.network.handler.SocketMessageHandler
import encore.network.messaging.socket.SocketMessage
import encore.network.messaging.socket.SocketMessageDispatcher
import testHelper.assertDoesNotFail
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SocketDispatcherTest {
    @Test
    fun `success dispatch of one message type, one handler, single expected message class`() {
        val dispatcher = SocketMessageDispatcher()
        val handler1 = Handler1()
        dispatcher.register(handler1)
        val dispatchResult = dispatcher.findHandlerFor(ExMsg1("asdf"))
        assertNotNull(dispatchResult.find { it.name == handler1.name })
    }

    @Test
    fun `dispatch routed correctly when multiple handlers with different type but same message`() {
        val dispatcher = SocketMessageDispatcher()
        val handler1 = Handler1()
        val handler4 = Handler4()
        dispatcher.register(handler1)
        dispatcher.register(handler4)
        val dispatchResult = dispatcher.findHandlerFor(ExMsg1("asdf"))
        assertNotNull(dispatchResult.find { it.name == handler1.name })
        assertNull(dispatchResult.find { it.name == handler4.name })
    }

    @Test
    fun `success dispatch of one message type, multiple handlers, same expected message class`() {
        val dispatcher = SocketMessageDispatcher()
        val handler1 = Handler1()
        val handler3 = Handler3()
        dispatcher.register(handler1)
        assertDoesNotFail {
            dispatcher.register(handler3)
        }
        val dispatchResult = dispatcher.findHandlerFor(ExMsg1("asdf"))
        assertNotNull(dispatchResult.find { it.name == handler1.name })
        assertNotNull(dispatchResult.find { it.name == handler3.name })
    }

    @Test
    fun `fail register of one message type, multiple handler, different expected message class`() {
        val dispatcher = SocketMessageDispatcher()
        dispatcher.register(Handler1())
        assertFailsWith<IllegalArgumentException> {
            dispatcher.register(Handler2())
        }
    }

}

class Handler1 : SocketMessageHandler<ExMsg1> {
    override val name: String = "Handler1"
    override val messageType: String = "type1"
    override val expectedMessageClass: KClass<ExMsg1> = ExMsg1::class
    override suspend fun handle(ctx: HandlerContext<ExMsg1>) {
        println("Handler1 - handle")
    }
}

class Handler2 : SocketMessageHandler<ExMsg2> {
    override val name: String = "Handler2"
    override val messageType: String = "type1"
    override val expectedMessageClass: KClass<ExMsg2> = ExMsg2::class
    override suspend fun handle(ctx: HandlerContext<ExMsg2>) {
        println("Handler2 - handle")
    }
}

class Handler3 : SocketMessageHandler<ExMsg1> {
    override val name: String = "Handler3"
    override val messageType: String = "type1"
    override val expectedMessageClass: KClass<ExMsg1> = ExMsg1::class
    override suspend fun handle(ctx: HandlerContext<ExMsg1>) {
        println("Handler3 - handle")
    }
}

class Handler4 : SocketMessageHandler<ExMsg1> {
    override val name: String = "Handler4"
    override val messageType: String = "type2"
    override val expectedMessageClass: KClass<ExMsg1> = ExMsg1::class
    override suspend fun handle(ctx: HandlerContext<ExMsg1>) {
        println("Handler4 - handle")
    }
}

class ExMsg1(val payload: String) : SocketMessage {
    override fun type(): String = "type1"
    override fun toString(): String = "ExMsg1($payload)"
}

class ExMsg2(val payload: String) : SocketMessage {
    override fun type(): String = "type1"
    override fun toString(): String = "ExMsg2($payload)"
}
