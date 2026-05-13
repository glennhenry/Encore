package encore.network.fanchant

import encore.network.fanchant.guide.CatchAllFanchantGuide

/**
 * [Fanchant] implementation for [CatchAllFanchantGuide].
 *
 * Behavior:
 * - [type] declares [CatchAllFanchantType] with a fixed identifier "N/A"
 *   (type is not applicable here).
 * - [toString] returns the decoded data.
 */
class CatchAllFanchant(val decoded: String) : Fanchant {
    override val type: FanchantType<*> = CatchAllFanchantType
    override fun toString(): String = decoded
}

object CatchAllFanchantType: FanchantType<CatchAllFanchant> {
    override val id: String = "N/A"
}
