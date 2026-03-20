package encore.server.handler.impl

import encore.server.handler.HandlerContext
import encore.server.handler.SocketMessageHandler
import encore.server.messaging.socket.SocketMessage
import encore.utils.logging.Fancam
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
        Fancam.warn { "No handler registered/implemented for type=${message.type()}" }
    }
}
