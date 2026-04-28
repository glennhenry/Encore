package encore.acts.photocard

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import encore.acts.photocard.model.Photocard
import encore.datastore.FieldPlayerId
import encore.datastore.ServerObjectsFilter
import encore.datastore.collection.PlayerId
import encore.datastore.collection.ServerObjects
import encore.datastore.runMongoCatching
import encore.datastore.throwIfNotModified
import kotlinx.coroutines.flow.firstOrNull

/**
 * Implementation of [PhotocardRepository] with Mongo.
 *
 * @property objects [ServerObjects] collection.
 */
class MongoPhotocardRepository(private val objects: MongoCollection<ServerObjects>) : PhotocardRepository {
    override val name: String = "MongoPhotocardRepository"

    override suspend fun getAllPhotocards(playerId: PlayerId): Result<List<Photocard>> {
        return runMongoCatching {
            val filter = Filters.and(
                ServerObjectsFilter,
                Filters.eq("acts.playerId", playerId)
            )

            val doc = objects
                .find(filter)
                .projection(Projections.elemMatch("acts", Filters.eq(FieldPlayerId, playerId)))
                .firstOrNull()

            val act = doc?.acts?.singleOrNull { it.playerId == playerId }
            act?.photocards ?: emptyList()
        }
    }

    override suspend fun deletePhotocard(playerId: PlayerId, actId: String): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.and(
                ServerObjectsFilter,
                Filters.eq("acts.playerId", playerId)
            )

            val update = Updates.pull(
                "acts.$[act].photocards",
                Filters.eq("actId", actId)
            )

            val options = UpdateOptions().arrayFilters(
                listOf(
                    Filters.eq("act.playerId", playerId)
                )
            )

            objects.updateOne(filter, update, options)
                .throwIfNotModified("deletePhotocard")
        }
    }

    override suspend fun savePhotocard(playerId: PlayerId, photocard: Photocard): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.and(
                ServerObjectsFilter,
                Filters.eq("acts.playerId", playerId)
            )

            val update = Updates.push("acts.$[act].photocards", photocard)

            val options = UpdateOptions().arrayFilters(
                listOf(
                    Filters.eq("act.playerId", playerId)
                )
            )

            objects.updateOne(filter, update, options)
                .throwIfNotModified("savePhotocard")
        }
    }
}
