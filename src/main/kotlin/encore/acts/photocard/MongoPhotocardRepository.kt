package encore.acts.photocard

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import encore.acts.photocard.model.Photocard
import encore.datastore.*
import encore.datastore.collection.PlayerId
import encore.datastore.collection.PlayerServerObjects
import encore.datastore.collection.ServerObjects
import kotlinx.coroutines.flow.firstOrNull
import org.bson.codecs.pojo.annotations.BsonId

/**
 * Implementation of [PhotocardRepository] with Mongo.
 *
 * @property psObj [PlayerServerObjects] collection.
 * @property sObj [ServerObjects] collection.
 */
class MongoPhotocardRepository(
    private val psObj: MongoCollection<PlayerServerObjects>,
    private val sObj: MongoCollection<ServerObjects>,
) : PhotocardRepository {
    override val name: String = "MongoPhotocardRepository"

    override suspend fun getAllPhotocards(playerId: PlayerId): Result<List<Photocard>> {
        return runMongoCatching {
            psObj
                .withDocumentClass<QueryPhotocard>()
                .find(Filters.eq(FieldPlayerId, playerId))
                .projection(
                    Projections.fields(
                        Projections.include(FieldPhotocards),
                        Projections.excludeId()
                    )
                )
                .firstOrNull()
                ?.photocards
        }
    }

    override suspend fun deletePhotocard(playerId: PlayerId, actId: String): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.eq(FieldPlayerId, playerId)
            val update = Updates.pull(FieldPhotocards, Filters.eq(FieldActId, actId))
            psObj.updateOne(filter, update)
                .throwIfNotModified("deletePhotocard", { filter }, { update })
        }
    }

    override suspend fun savePhotocard(playerId: PlayerId, photocard: Photocard): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.eq(FieldPlayerId, playerId)
            val update = Updates.combine(
                Updates.setOnInsert(FieldPlayerId, playerId),
                Updates.push(FieldPhotocards, photocard)
            )
            // upsert is actually not needed on runtime since account creation pre-inserts
            val options = UpdateOptions().upsert(true)
            psObj.updateOne(filter, update, options)
                .throwIfNotModified("savePhotocard", { filter }, { update })
        }
    }

    override suspend fun updatePhotocard(
        playerId: PlayerId,
        photocard: Photocard
    ): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.eq(FieldPlayerId, playerId)
            val update = Updates.set("$FieldPhotocards.$[photocard]", photocard)
            val options = UpdateOptions().arrayFilters(
                listOf(Filters.eq("photocard.$FieldActId", photocard.actId))
            )
            psObj.updateOne(filter, update, options)
                .throwIfNotModified("updatePhotocard", { filter }, { update })
        }
    }

    override suspend fun getServerPhotocards(): Result<List<Photocard>> {
        return runMongoCatching {
            sObj
                .withDocumentClass<QueryPhotocard>()
                .find(ServerObjectsFilter)
                .projection(
                    Projections.fields(
                        Projections.include(FieldPhotocards),
                        Projections.excludeId()
                    )
                )
                .firstOrNull()
                ?.photocards
        }
    }

    override suspend fun deleteServerPhotocard(actId: String): Result<Unit> {
        return runMongoCatching {
            val update = Updates.pull(FieldPhotocards, Filters.eq(FieldActId, actId))
            sObj.updateOne(ServerObjectsFilter, update)
                .throwIfNotModified("deleteServerPhotocard", null, { update })
        }
    }

    override suspend fun saveServerPhotocard(photocard: Photocard): Result<Unit> {
        return runMongoCatching {
            val update = Updates.push(FieldPhotocards, photocard)
            sObj.updateOne(ServerObjectsFilter, update)
                .throwIfNotModified("saveServerPhotocard", null, { update })
        }
    }

    override suspend fun updateServerPhotocard(photocard: Photocard): Result<Unit> {
        return runMongoCatching {
            val filter = ServerObjectsFilter
            val update = Updates.set("$FieldPhotocards.$[photocard]", photocard)
            val options = UpdateOptions().arrayFilters(
                listOf(Filters.eq("photocard.$FieldActId", photocard.actId))
            )
            sObj.updateOne(filter, update, options)
                .throwIfNotModified("updateServerPhotocard", { filter }, { update })
        }
    }
}

/**
 * Mongo projection class to query the [PlayerServerObjects.photocards].
 */
data class QueryPhotocard(
    @field:BsonId val id: String? = null,
    val photocards: List<Photocard>
)
