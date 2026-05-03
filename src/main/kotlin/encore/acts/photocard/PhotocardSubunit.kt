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
                Fancam.error(it) { "Failed to get all photocards for $playerId" }
            }
            .getOrNull() ?: emptyList()
    }

    suspend fun deletePhotocard(playerId: PlayerId, actId: String) {
        photocardRepository.deletePhotocard(playerId, actId)
            .onFailure {
                Fancam.error(it) { "Failed to delete photocard for playerId=$playerId with actId=$actId" }
            }
    }

    suspend fun savePhotocard(playerId: PlayerId, photocard: Photocard) {
        photocardRepository.savePhotocard(playerId, photocard)
            .onFailure {
                Fancam.error(it) { "Failed to save photocard for name=${photocard.name}, playerId=$playerId, actId=${photocard.actId}" }
            }
    }

    suspend fun updatePhotocard(playerId: PlayerId, photocard: Photocard) {
        photocardRepository.updatePhotocard(playerId, photocard)
            .onFailure {
                Fancam.error(it) { "Failed to update photocard for name=${photocard.name}, playerId=$playerId, actId=${photocard.actId}" }
            }
    }

    suspend fun getServerPhotocards(): List<Photocard> {
        return photocardRepository.getServerPhotocards()
            .onFailure {
                Fancam.error(it) { "Failed to get all server photocards." }
            }
            .getOrNull() ?: emptyList()
    }

    suspend fun deleteServerPhotocard(actId: String) {
        photocardRepository.deleteServerPhotocard(actId)
            .onFailure {
                Fancam.error(it) { "Failed to delete server photocard with actId=$actId" }
            }
    }

    suspend fun saveServerPhotocard(photocard: Photocard) {
        photocardRepository.saveServerPhotocard(photocard)
            .onFailure {
                Fancam.error(it) { "Failed to save server photocard with name=${photocard.name}, actId=${photocard.actId}" }
            }
    }

    suspend fun updateServerPhotocard(photocard: Photocard) {
        photocardRepository.updateServerPhotocard(photocard)
            .onFailure {
                Fancam.error(it) { "Failed to update server photocard with name=${photocard.name}, actId=${photocard.actId}" }
            }
    }

    override suspend fun debut(scope: ServerScope): Result<Unit> = Result.success(Unit)
    override suspend fun disband(scope: ServerScope): Result<Unit> = Result.success(Unit)

    companion object {
        /**
         * Creates a test instance of [PhotocardSubunit].
         *
         * @param photocardRepository repository dependency.
         * Use [BlankPhotocardRepository] when this subunit is not used.
         */
        fun createForTest(photocardRepository: PhotocardRepository = BlankPhotocardRepository()): PhotocardSubunit {
            return PhotocardSubunit(photocardRepository)
        }
    }
}
