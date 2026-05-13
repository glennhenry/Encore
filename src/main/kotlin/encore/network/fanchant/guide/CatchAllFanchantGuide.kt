package encore.network.fanchant.guide

import encore.network.fanchant.Fanchant
import encore.utils.safeAsciiString
import encore.network.fanchant.CatchAllFanchant

/**
 * A fallback-based implementation of [FanchantGuide].
 *
 * This guide focuses on catching all unknown or unsupported message.
 * It guarantees that any incoming packet is always produced and reported.
 * It will only be used when no other `FanchantGuide` are able to decode successfully.
 *
 * Behavior:
 * - [verify] always returns `true`.
 * - [tryDecode] always succeeds, raw bytes are converted into string via [safeAsciiString].
 * - [materialize] wraps the decoded string into a [CatchAllFanchant].
 */
class CatchAllFanchantGuide : FanchantGuide<String> {
    override val name: String = "DefaultFormat"

    override fun verify(data: ByteArray): Boolean = true

    override fun tryDecode(data: ByteArray): DecodeResult<String> {
        return DecodeResult.Success(data.safeAsciiString())
    }

    override fun materialize(decoded: String): Fanchant {
        return CatchAllFanchant(decoded)
    }
}
