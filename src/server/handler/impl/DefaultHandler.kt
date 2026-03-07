package server.handler.impl

import server.handler.HandlerContext
import server.handler.SocketMessageHandler
import server.messaging.socket.SocketMessage
import utils.logging.Logger
import kotlin.reflect.KClass

/**
 * Default handler as the fallback for any unregistered socket handlers.
 */
class DefaultHandler : SocketMessageHandler<SocketMessage> {
    override val name: String = "DefaultHandler"
    override val messageType: String = "Default"
    override val expectedMessageClass: KClass<SocketMessage> = SocketMessage::class

    override fun shouldHandle(message: SocketMessage): Boolean = true

    override suspend fun handle(ctx: HandlerContext<SocketMessage>) = with(ctx) {
        Logger.warn { "No handler registered/implemented for type=${message.type()}" }
    }
}
