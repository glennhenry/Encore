package encore.network.handler

import encore.fancam.Fancam
import encore.network.fanchant.CatchAllFanchant
import encore.network.fanchant.CatchAllFanchantType
import encore.network.fanchant.FanchantType

/**
 * Fallback-based handler implementation of [FanchantHandler].
 *
 * This is used to catch all unrecognized, unsupported, or unregistered
 * fanchant types. It ensures any kind of packets get received and logged.
 *
 * It can also be used as a place to quickly prototype a response without
 * actually implementing a strict fanchant guide or fanchant class.
 */
class CatchAllHandler : FanchantHandler<CatchAllFanchant> {
    override val fanchantType: FanchantType<CatchAllFanchant> = CatchAllFanchantType

    override suspend fun handle(ctx: HandlerContext<CatchAllFanchant>) = with(ctx) {
        Fancam.warn { "No handler registered/implemented for type=${fanchant.type.id}" }

        // directly respond here...
    }
}
