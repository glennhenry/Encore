package server.messaging.format

import server.messaging.socket.SocketMessage

/**
 * Describes a network message format recognized by the server.
 *
 * A [MessageFormat] defines how a sequence of raw bytes can be interpreted
 * as a particular message structure. It consists of:
 *
 * 1. A lightweight [verify] step used for fast, permissive filtering.
 * 2. A full decoding step [tryDecode], which may succeed or fail.
 * 3. A materialization step [materialize] that produces a [SocketMessage]
 *    suitable for handler dispatch.
 *
 * See `test.kotlin.example.ExampleFormatTest` for implementation example.
 *
 * @param T The intermediate decoded representation produced by this format.
 */
interface MessageFormat<T> {

    /**
     * Name of the message format used for debugging and logging.
     */
    val name: String

    /**
     * Performs a cheap, permissive check to determine whether the raw byte
     * sequence [data] *may* conform to this message format.
     *
     * Implementations must be fast and non-strict. False positives are
     * acceptable and expected.
     *
     * This method must not perform full decoding or heavy parsing.
     *
     * Examples:
     * - A JSON-based format may check whether the first byte is '{'
     *   and the last byte is '}'.
     * - A framed protocol may verify that the message length is consistent.
     * - A fixed-header protocol may check for the presence of a known
     *   header signature.
     */
    fun verify(data: ByteArray): Boolean

    /**
     * Attempts to fully decode the raw byte sequence [data].
     *
     * Decoding may fail even if [verify] returned true. Such failures
     * are considered normal and non-fatal.
     *
     * In systems supporting multiple message formats, it is acceptable
     * for multiple formats to verify the same input. Ideally, only one
     * format should successfully decode it; multiple successful decodes
     * is treated as ambiguity and will be reported.
     *
     * @return A [DecodeResult] indicating success or failure.
     */
    fun tryDecode(data: ByteArray): DecodeResult<T>

    /**
     * Converts the decoded intermediate representation into a concrete
     * [SocketMessage] that can be dispatched to handlers.
     *
     * This step bridges protocol-level data with application-level
     * message semantics. Implementations may produce:
     * - A generic message wrapper, or
     * - A strongly typed, domain-specific message.
     *
     * This method works together with [materializeAny] which
     * contain an unchecked cast to bypass star projection.
     * This is safe if [decoded] is produced from [tryDecode].
     */
    fun materialize(decoded: T): SocketMessage

    @Suppress("UNCHECKED_CAST")
    fun materializeAny(decoded: Any?): SocketMessage {
        return materialize(decoded as T)
    }
}

/**
 * Represents the result of attempting to decode a message on a
 * [MessageFormat].
 *
 * @param T The type of the successfully decoded message.
 */
sealed interface DecodeResult<out T> {

    /**
     * Indicates a successful decoding outcome.
     *
     * In an ideal system, exactly one message format should succeed
     * in decoding a given message. Multiple successes
     * indicate ambiguity and will be reported.
     */
    data class Success<T>(val value: T) : DecodeResult<T>

    /**
     * Indicates that decoding failed.
     *
     * Decode failures are expected and generally harmless, as long as
     * another message format successfully decodes the same input.
     *
     * @property reason Optional explanation for the failure.
     * @property error Optional underlying exception that caused the failure.
     */
    data class Failure(
        val reason: String? = null,
        val error: Throwable? = null
    ) : DecodeResult<Nothing>
}
