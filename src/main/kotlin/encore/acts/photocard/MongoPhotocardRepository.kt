package encore.acts.photocard

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import encore.acts.photocard.model.Photocard
import encore.datastore.*
import encore.datastore.collection.PlayerId
import encore.datastore.collection.ServerObjects
import kotlinx.coroutines.flow.firstOrNull
import org.bson.codecs.pojo.annotations.BsonId

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

    override suspend fun updatePhotocard(
        playerId: PlayerId,
        photocard: Photocard
    ): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.and(
                ServerObjectsFilter,
                Filters.eq("acts.playerId", playerId)
            )

            val update = Updates.set("acts.$[act].photocards.$[photocard]", photocard)

            val options = UpdateOptions().arrayFilters(
                listOf(
                    Filters.eq("act.playerId", playerId),
                    Filters.eq("photocard.actId", photocard.actId)
                )
            )

            objects.updateOne(filter, update, options)
                .throwIfNotModified("updatePhotocard")
        }
    }

    override suspend fun getServerPhotocards(): Result<List<Photocard>> {
        return runMongoCatching {
            objects
                .withDocumentClass<QueryServerActs>()
                .find(ServerObjectsFilter)
                .projection(
                    Projections.fields(
                        Projections.include(FieldServerActs),
                        Projections.excludeId()
                    )
                )
                .firstOrNull()
                ?.serverActs
        }
    }

    override suspend fun deleteServerPhotocard(actId: String): Result<Unit> {
        return runMongoCatching {
            val update = Updates.pull("serverActs", Filters.eq("actId", actId))
            objects.updateOne(ServerObjectsFilter, update)
                .throwIfNotModified("deleteServerPhotocard")
        }
    }

    override suspend fun saveServerPhotocard(photocard: Photocard): Result<Unit> {
        return runMongoCatching {
            val update = Updates.push("serverActs", photocard)
            objects.updateOne(ServerObjectsFilter, update)
                .throwIfNotModified("saveServerPhotocard")
        }
    }

    override suspend fun updateServerPhotocard(photocard: Photocard): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.and(
                ServerObjectsFilter,
                Filters.eq("serverActs.actId", photocard.actId)
            )

            val update = Updates.set("serverActs.$", photocard)
            objects.updateOne(filter, update)
                .throwIfNotModified("updateServerPhotocard")
        }
    }
}

/**
 * Mongo projection class to query the [ServerObjects.serverActs].
 */
data class QueryServerActs(
    @field:BsonId val id: String? = null,
    val serverActs: List<Photocard>
)
