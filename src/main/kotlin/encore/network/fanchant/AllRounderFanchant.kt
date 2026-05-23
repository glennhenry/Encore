package encore.network.fanchant

import encore.network.fanchant.guide.AllRounderFanchantGuide

/**
 * [Fanchant] implementation for [AllRounderFanchantGuide].
 *
 * Behavior:
 * - [type] declares [AllRounderFanchantType] with a fixed identifier "N/A"
 *   (type is not applicable here).
 * - [toString] returns the decoded data.
 */
class AllRounderFanchant(val decoded: String) : Fanchant {
    override val type: FanchantType<*> = AllRounderFanchantType
    override fun toString(): String = decoded
}

object AllRounderFanchantType: FanchantType<AllRounderFanchant> {
    override val id: String = "N/A"
}
