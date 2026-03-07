package core.data.resources

/**
 * Represents a static game resource used for loading and storing game data.
 *
 * Game data can be stored in various formats such as JSON, XML, or others.
 * This interface abstracts the underlying data format and implementation can provide
 * access to its raw representation.
 */
interface GameResource {
    val name: String

    /**
     * Path to the resource file.
     */
    val path: String

    /**
     * Reads the content as a UTF-8 string (for text-based formats).
     */
    fun readText(): String

    /**
     * Reads the raw content of this resource as a byte array.
     */
    fun readBytes(): ByteArray
}
