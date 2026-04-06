package encore.datastore

import com.mongodb.client.result.UpdateResult

/**
 * Thrown when a MongoDB query expected a document but found none.
 */
class DocumentNotFoundException(message: String) : RuntimeException(message)

/**
 * Thrown when a MongoDB update operation fails to update
 * the specified document (i.e., when `modifiedCount < 1`).
 */
class DocumentNotUpdatedException(playerId: String) : RuntimeException("Player not updated: $playerId")

/**
 * Executes [block] and wraps its result in a [Result].
 *
 * This is typically used for queries such as find one that may return `null`
 * (e.g. by calling `firstOrNull`).
 *
 * Behavior:
 * - Returns [Result.success] if [block] returns a non-null value.
 * - Returns [Result.failure] with [NoSuchElementException] if the result is null.
 * - Returns [Result.failure] if [block] throws.
 *
 * @param message message used when the result is null
 */
inline fun <T> runMongoCatching(
    message: String = "Expected document to exist",
    block: () -> T?
): Result<T> = runCatching {
    block() ?: throw DocumentNotFoundException(message)
}

/**
 * Check whether update operation were matched and modified; and throw an error
 * if it's not.
 *
 * @param context Information about the update operation and will be included in the exception.
 * @throws DocumentNotFoundException if `matchedCount` is less than 1
 * @throws DocumentNotUpdatedException if `modifiedCount` is less than 1
 */
fun UpdateResult.throwIfNotModified(context: String = "") {
    if (matchedCount < 1) {
        throw DocumentNotFoundException("No document matched: $context")
    }
    if (modifiedCount < 1) {
        throw DocumentNotUpdatedException("Document not modified (matchedCount=$matchedCount): $context")
    }
}
