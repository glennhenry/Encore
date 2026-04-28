package encore.acts.photocard

import encore.acts.photocard.model.Photocard
import encore.datastore.collection.PlayerId
import encore.fancam.Fancam
import encore.subunit.Subunit
import encore.subunit.scope.ServerScope

/**
 * Server subunit that provides access to [Photocard]s of players.
 *
 * @property photocardRepository [PhotocardRepository] implementation.
 */
class PhotocardSubunit(private val photocardRepository: PhotocardRepository) : Subunit<ServerScope> {
    suspend fun getAllPhotocards(playerId: PlayerId): List<Photocard> {
        return photocardRepository.getAllPhotocards(playerId)
            .onFailure {
                Fancam.error { "Failed to get all photocards for $playerId" }
            }
            .getOrNull() ?: emptyList()
    }

    suspend fun deletePhotocard(playerId: PlayerId, actId: String) {
        photocardRepository.deletePhotocard(playerId, actId)
            .onFailure {
                Fancam.error { "Failed to delete photocard for playerId=$playerId with actId=$actId" }
            }
    }

    suspend fun savePhotocard(playerId: PlayerId, photocard: Photocard) {
        photocardRepository.savePhotocard(playerId, photocard)
            .onFailure {
                Fancam.error { "Failed to save photocard for playerId=$playerId with actId=${photocard.actId} name=${photocard.name}" }
            }
    }

    override suspend fun debut(scope: ServerScope): Result<Unit> = Result.success(Unit)
    override suspend fun disband(scope: ServerScope): Result<Unit> = Result.success(Unit)
}
