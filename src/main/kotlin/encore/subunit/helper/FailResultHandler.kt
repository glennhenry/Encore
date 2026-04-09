package encore.subunit.helper

import encore.datastore.DocumentNotFoundException
import encore.datastore.DocumentNotUpdatedException
import encore.fancam.Fancam

/**
 * Handles a failed [Result] in the context of a subunit's **get/retrieval operations**.
 *
 * Subunits often call repository functions that return [Result] types. When a
 * repository operation fails, it wraps exceptions (e.g., [DocumentNotFoundException])
 * in the `Result`. Subunits then need to handle these exceptions, which can lead
 * to repetitive boilerplate.
 *
 * This helper provides a concise way to:
 *   1. Log domain-specific messages for known exceptions (like `DocumentNotFoundException`).
 *   2. Log a default or custom message for unknown exceptions.
 *
 * Usage:
 * ```
 * result.onFailure {
 *     it.handleGet(
 *         notFoundMessage = { "Items not found for player $playerId" },
 *         unknownMessage  = { "Failed to fetch items for player $playerId" }
 *     )
 * }
 * ```
 *
 * @param notFoundMessage Message to log when a [DocumentNotFoundException] is thrown
 * @param unknownMessage Message to log when any other unknown exception is thrown
 */
inline fun <T> Result<T>.failHandleGet(
    crossinline notFoundMessage: () -> String,
    crossinline unknownMessage: () -> String = { "Unknown error" }
) {
    this.exceptionOrNull()?.let { throwable ->
        when (throwable) {
            is DocumentNotFoundException -> Fancam.error { notFoundMessage() }
            else -> Fancam.error(throwable) { unknownMessage() }
        }
    }
}

/**
 * Handles a failed [Result] in the context of a subunit's **update operations**.
 *
 * Similar to [failHandleGet], but designed for update operations which adds
 * additional matching for [DocumentNotUpdatedException].
 *
 * Usage:
 * ```
 * result.onFailure {
 *     it.handleGet(
 *         notFoundMessage = { "Items not found for player $playerId" },
 *         notUpdatedMessage = { "Failed to update role for player $playerId" },
 *         unknownMessage  = { "Failed to fetch items for player $playerId" }
 *     )
 * }
 * ```
 *
 * @param notFoundMessage Message to log when a [DocumentNotFoundException] is thrown
 * @param notUpdatedMessage Message to log when a [DocumentNotUpdatedException] is thrown
 * @param unknownMessage Message to log when any other unknown exception is thrown
 */
inline fun <T> Result<T>.failHandleUpdate(
    crossinline notFoundMessage: () -> String,
    crossinline notUpdatedMessage: () -> String,
    crossinline unknownMessage: () -> String = { "Unknown error" }
) {
    this.exceptionOrNull()?.let { throwable ->
        when (throwable) {
            is DocumentNotFoundException -> Fancam.error { notFoundMessage() }
            is DocumentNotUpdatedException -> Fancam.error { notUpdatedMessage() }
            else -> Fancam.error(throwable) { unknownMessage() }
        }
    }
}
