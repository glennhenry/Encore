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
     * - [Result.failure] when there is an internal error or photocard is not found.
     * - [Result.success] otherwise.
     */
    suspend fun deletePhotocard(playerId: PlayerId, actId: String): Result<Unit>

    /**
     * Save the [photocard] for [playerId] which adds a new entry.
     * @return [Result] type of whether the operation succeed or fails.
     * - [Result.failure] when there is an internal error.
     * - [Result.success] otherwise.
     */
    suspend fun savePhotocard(playerId: PlayerId, photocard: Photocard): Result<Unit>

    /**
     * Update the existing [photocard] for [playerId].
     * @return [Result] type of whether the operation succeed or fails.
     * - [Result.failure] when there is an internal error or photocard is not found.
     * - [Result.success] otherwise.
     */
    suspend fun updatePhotocard(playerId: PlayerId, photocard: Photocard): Result<Unit>

    /**
     * Get all server-owned photocards.
     * @return Every photocards in [Result] type.
     * - [Result.failure] when there is an internal repo/DB error.
     * - [Result.success] otherwise, including case when photocards are empty.
     */
    suspend fun getServerPhotocards(): Result<List<Photocard>>

    /**
     * Delete the server-owned [Photocard] associated with [actId].
     * @return [Result] type of whether the operation succeed or fails.
     * - [Result.failure] when there is an internal error or photocard is not found.
     * - [Result.success] otherwise.
     */
    suspend fun deleteServerPhotocard(actId: String): Result<Unit>

    /**
     * Save a server-owned [photocard] which adds a new entry.
     * @return [Result] type of whether the operation succeed or fails.
     * - [Result.failure] when there is an internal error.
     * - [Result.success] otherwise.
     */
    suspend fun saveServerPhotocard(photocard: Photocard): Result<Unit>

    /**
     * Update the existing server-owned [photocard].
     * @return [Result] type of whether the operation succeed or fails.
     * - [Result.failure] when there is an internal error or photocard is not found.
     * - [Result.success] otherwise.
     */
    suspend fun updateServerPhotocard(photocard: Photocard): Result<Unit>
}
