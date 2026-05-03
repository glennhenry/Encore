package encore.acts.photocard

import encore.acts.photocard.model.Photocard
import encore.datastore.collection.PlayerId
import encore.datastore.collection.ServerId

/**
 * In-memory implementation of [PhotocardRepository].
 */
class InMemoryPhotocardRepository(
    initPhotocards: MutableMap<String, MutableList<Photocard>>.() -> Unit = {}
) :
    PhotocardRepository {
    override val name: String = "InMemoryPhotocardRepository"

    private val photocards = mutableMapOf<String, MutableList<Photocard>>()

    init {
        initPhotocards.invoke(photocards)
    }

    override suspend fun getAllPhotocards(playerId: PlayerId): Result<List<Photocard>> {
        return Result.success(photocards.getOrDefault(playerId, emptyList()))
    }

    override suspend fun deletePhotocard(
        playerId: PlayerId,
        actId: String
    ): Result<Unit> {
        photocards[playerId] ?: return pcsNotFound(playerId)
        val removed = photocards[playerId]?.removeIf { it.actId == actId }

        if (removed == null || !removed) {
            return pcNotFound(playerId, actId)
        }

        return Result.success(Unit)
    }

    override suspend fun savePhotocard(
        playerId: PlayerId,
        photocard: Photocard
    ): Result<Unit> {
        val current = photocards.getOrDefault(playerId, mutableListOf())
        current.add(photocard)
        photocards[playerId] = current
        return Result.success(Unit)
    }

    override suspend fun updatePhotocard(
        playerId: PlayerId,
        photocard: Photocard
    ): Result<Unit> {
        val pcs = photocards[playerId] ?: return pcsNotFound(playerId)

        var idx = -1
        for (i in pcs.indices) {
            if (pcs[i].actId == photocard.actId) {
                idx = i
                break
            }
        }

        if (idx == -1) {
            return pcNotFound(playerId, photocard.actId)
        }

        photocards[playerId]?.set(idx, photocard)
        return Result.success(Unit)
    }

    override suspend fun getServerPhotocards(): Result<List<Photocard>> {
        return Result.success(photocards[ServerId] ?: emptyList())
    }

    override suspend fun deleteServerPhotocard(actId: String): Result<Unit> {
        photocards[ServerId] ?: return pcsNotFound(ServerId)
        val removed = photocards[ServerId]?.removeIf { it.actId == actId }

        if (removed == null || !removed) {
            return pcNotFound(ServerId, actId)
        }

        return Result.success(Unit)
    }

    override suspend fun saveServerPhotocard(photocard: Photocard): Result<Unit> {
        val current = photocards.getOrDefault(ServerId, mutableListOf())
        current.add(photocard)
        photocards[ServerId] = current
        return Result.success(Unit)
    }

    override suspend fun updateServerPhotocard(photocard: Photocard): Result<Unit> {
        val pcs = photocards[ServerId] ?: return pcsNotFound(ServerId)

        var idx = -1
        for (i in pcs.indices) {
            if (pcs[i].actId == photocard.actId) {
                idx = i
                break
            }
        }

        if (idx == -1) {
            return pcNotFound(ServerId, photocard.actId)
        }

        photocards[ServerId]?.set(idx, photocard)
        return Result.success(Unit)
    }

    private fun pcsNotFound(playerId: String): Result<Unit> {
        return Result.failure(IllegalStateException("Photocards is not found for $playerId"))
    }

    private fun pcNotFound(playerId: String, actId: String): Result<Unit> {
        return Result.failure(IllegalStateException("Photocard $actId is not found for $playerId"))
    }
}
