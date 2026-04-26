package encore.network.handler

import encore.fancam.Fancam
import encore.network.messaging.socket.DefaultMessage
import kotlin.reflect.KClass

/**
 * Default handler as the fallback for any unregistered socket handlers.
 */
class DefaultHandler : SocketMessageHandler<DefaultMessage> {
    override val name: String = "DefaultHandler"
    override val messageType: String = "Default"
    override val expectedMessageClass: KClass<DefaultMessage> = DefaultMessage::class

    override fun shouldHandle(message: DefaultMessage): Boolean = true

    override suspend fun handle(ctx: HandlerContext<DefaultMessage>) = with(ctx) {
        Fancam.warn { "No handler registered/implemented for type=${message.type()}" }
    }
}