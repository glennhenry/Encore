package encore.datastore

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.InsertManyResult
import com.mongodb.client.result.InsertOneResult
import com.mongodb.client.result.UpdateResult
import org.bson.conversions.Bson

/**
 * Thrown when a MongoDB query expected a document but found none.
 */
class DocumentNotFoundException(message: String) : RuntimeException(message)

/**
 * Thrown when a MongoDB update operation fails to update
 * the specified document (i.e., when `modifiedCount < 1`).
 */
class DocumentNotUpdatedException(message: String) : RuntimeException(message)

/**
 * Executes [block] and wraps its result in a [Result].
 *
 * This is typically used for queries such as find one that may return `null`
 * (e.g. by calling `firstOrNull`).
 *
 * Behavior:
 * - Returns [Result.success] if [block] returns a non-null value.
 * - Returns [Result.failure] with [DocumentNotFoundException] if the result is null.
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
 * @param filter Optional lambda containing the Mongo `Filters` which will be included in the exception.
 * @param update Optional lambda containing the Mongo `Update` which will be included in the exception.
 * @throws DocumentNotFoundException if `matchedCount` is less than 1
 * @throws DocumentNotUpdatedException if `modifiedCount` is less than 1
 */
fun UpdateResult.throwIfNotModified(
    context: String = "",
    filter: (() -> Bson?)? = null,
    update: (() -> Bson?)? = null,
) {
    if (this.upsertedId == null && matchedCount < 1) {
        val filterStr = filter?.invoke()?.toBsonDocument()?.toJson()
        throw DocumentNotFoundException(
            "No document matched: $context\n" +
                    (filterStr?.let { "     filter=$it" } ?: "")
        )
    }

    if (this.upsertedId == null && modifiedCount < 1) {
        val filterStr = filter?.invoke()?.toBsonDocument()?.toJson()
        val updateStr = update?.invoke()?.toBsonDocument()?.toJson()
        throw DocumentNotUpdatedException(
            "Document matched but not updated: $context\n" +
                    (filterStr?.let { "     filter=$it" } ?: "") +
                    (updateStr?.let { "\n     update=$it" } ?: "")
        )
    }
}

/**
 * Ensure this insert one result is acknowledged, otherwise
 * throw an [IllegalStateException].
 *
 * Can be chained with [InsertOneResult.and].
 */
fun ensureAck(res: InsertOneResult): InsertOneResult {
    if (!res.wasAcknowledged()) {
        error("MongoDB insert one not acknowledged: $res")
    }
    return res
}

/**
 * Ensure this insert one result and [other] is acknowledged,
 * otherwise throw an IllegalStateException.
 */
fun InsertOneResult.and(other: InsertOneResult): InsertOneResult {
    if (!this.wasAcknowledged() || !other.wasAcknowledged()) {
        error("MongoDB insert one not acknowledged: \n" +
                "this=$this \n" +
                "other=$other")
    }
    return other
}

/**
 * Ensure this insert many result is acknowledged, otherwise
 * throw an [IllegalStateException].
 *
 * Can be chained with [InsertManyResult.and].
 */
fun ensureAck(res: InsertManyResult): InsertManyResult {
    if (!res.wasAcknowledged()) {
        error("MongoDB insert many not acknowledged: $res")
    }
    return res
}

/**
 * Ensure this insert many result and [other] is acknowledged,
 * otherwise throw an [IllegalStateException].
 */
fun InsertManyResult.and(other: InsertManyResult): InsertManyResult {
    if (!this.wasAcknowledged() || !other.wasAcknowledged()) {
        error("MongoDB insert many not acknowledged: \n" +
                "this=$this \n" +
                "other=$other")
    }
    return other
}

/**
 * Ensure this delete result is acknowledged, otherwise
 * throw an [IllegalStateException].
 *
 * Can be chained with [DeleteResult.and].
 */
fun ensureAck(res: DeleteResult): DeleteResult {
    if (!res.wasAcknowledged()) {
        error("MongoDB delete not acknowledged: $res")
    }
    return res
}

/**
 * Ensure this delete result and [other] is acknowledged,
 * otherwise throw an [IllegalStateException].
 */
fun DeleteResult.and(other: DeleteResult): DeleteResult {
    if (!this.wasAcknowledged() || !other.wasAcknowledged()) {
        error("MongoDB delete not acknowledged: \n" +
                "this=$this \n" +
                "other=$other")
    }
    return other
}
