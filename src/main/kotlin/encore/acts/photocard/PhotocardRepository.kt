package encore.acts.photocard

import encore.acts.photocard.model.Photocard
import encore.datastore.collection.PlayerId
import encore.repository.Repository

/**
 * Repository for [Photocard].
 *
 * Implementation provides access to the photocards of each [PlayerId].
 */
interface PhotocardRepository : Repository {
    /**
     * Get all photocards for [playerId].
     * @return Every photocards in [Result] type.
     * - [Result.failure] when there is an internal repo/DB error.
     * - [Result.success] otherwise, including case when photocards are empty.
     */
    suspend fun getAllPhotocards(playerId: PlayerId): Result<List<Photocard>>

    /**
     * Delete the [Photocard] associated with [actId] for [playerId].
     * @return [Result] type of whether the operation succeed or fails.
     */
    suspend fun deletePhotocard(playerId: PlayerId, actId: String): Result<Unit>

    /**
     * Save the [photocard] for [playerId].
     * @return [Result] type of whether the operation succeed or fails.
     */
    suspend fun savePhotocard(playerId: PlayerId, photocard: Photocard): Result<Unit>
}
