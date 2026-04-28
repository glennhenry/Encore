package encore.acts.photocard

import encore.acts.photocard.model.Photocard
import encore.datastore.collection.PlayerId

class BlankPhotocardRepository: PhotocardRepository {
    override val name: String = "BlankPhotocardRepository"
    override suspend fun getAllPhotocards(playerId: PlayerId): Result<List<Photocard>> = TODO("NO OPERATION")
    override suspend fun deletePhotocard(playerId: PlayerId, actId: String): Result<Unit> = TODO("NO OPERATION")
    override suspend fun savePhotocard(playerId: PlayerId, photocard: Photocard): Result<Unit> = TODO("NO OPERATION")
}
