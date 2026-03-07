package server.messaging.format

import server.messaging.socket.SocketMessage
import utils.functions.safeAsciiString
import server.messaging.socket.DefaultMessage

/**
 * Default [MessageFormat] that decodes any incoming message as a UTF-8 [String].
 *
 * This format acts as a fallback for unknown or unsupported message structures,
 * and will only be used when nothing else is matched. It guarantees that any
 * incoming data is always produced and reported.
 *
 * Behavior:
 * - [verify] always returns `true`.
 * - [tryDecode] always succeeds, raw bytes are converted into string.
 * - [materialize] wraps the decoded string into a [DefaultMessage].
 */
class DefaultFormat : MessageFormat<String> {
    override val name: String = "DefaultFormat"

    override fun verify(data: ByteArray): Boolean = true

    override fun tryDecode(data: ByteArray): DecodeResult<String> {
        return DecodeResult.Success(data.safeAsciiString())
    }

    override fun materialize(decoded: String): SocketMessage {
        return DefaultMessage(decoded)
    }
}
