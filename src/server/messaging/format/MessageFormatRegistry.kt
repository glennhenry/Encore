package server.messaging.format

import utils.logging.Logger

/**
 * Registry of all [MessageFormat]s supported by the server.
 *
 * The registry is responsible for:
 * - Keeping track of registered message formats.
 * - Identifying which formats *may* match a given raw byte sequence.
 * - Providing a safe fallback when no format matches.
 *
 * Message format identification is intentionally permissive
 * and based on [MessageFormat.verify].
 */
class MessageFormatRegistry {
    private val formats = mutableListOf<MessageFormat<*>>()
    private val default = DefaultFormat()

    /**
     * Registers a new [MessageFormat] to be considered during format detection.
     */
    fun register(format: MessageFormat<*>) {
        formats.add(format)
    }

    /**
     * Identifies message formats that *may* correspond to the raw byte
     * sequence [data].
     *
     * This method applies only the lightweight [MessageFormat.verify]
     * check and does not perform full decoding.
     *
     * Verification errors are caught and logged to prevent malformed or
     * experimental formats from disrupting format detection.
     *
     * @return A list of candidate [MessageFormat]s that may match the data.
     *         If no formats match, a fallback [DefaultFormat] is returned.
     */
    fun identifyFormat(data: ByteArray): List<MessageFormat<*>> {
        val matched = mutableListOf<MessageFormat<*>>()

        for (format in formats) {
            try {
                if (format.verify(data)) {
                    matched.add(format)
                }
            } catch (e: Exception) {
                Logger.verbose {
                    val peek = data.copyOfRange(0, minOf(20, data.size))
                    "${format.name} verify failed; peek=$peek; error=$e"
                }
            }
        }

        return matched.ifEmpty { listOf(default) }
    }
}
