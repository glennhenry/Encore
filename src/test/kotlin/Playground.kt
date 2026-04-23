import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.kotlin.client.coroutine.MongoCollection
import encore.context.PlayerSubunits
import encore.context.ServerContext
import encore.datastore.FieldPlayerId
import encore.datastore.ServerObjectsFilter
import encore.datastore.collection.ServerObjectsId
import encore.datastore.runMongoCatching
import encore.fancam.Fancam
import encore.repository.Repository
import encore.network.transport.Connection
import encore.tasks.CancellationReason
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * Playground for quick testing and code run
 *
 * .\gradlew test --tests "Playground.playground" --console=plain
 */
@Ignore
class Playground {
    @Test
    fun playground() {
        for (i in 0 until 257) {
            println("\u001B[48;5;${i}m THIS IS $i")
        }

        throw Exception()
    }
}



interface StageAct<T : ActConcept> {
    val name: String

    suspend fun createId(concept: T): String
    suspend fun createIdentity(concept: T): Map<String, String> = emptyMap()
    suspend fun createSetup(concept: T): ActSetup

    suspend fun onStart(concept: T) = Unit
    suspend fun perform(concept: T, times: Int)
    suspend fun onEndingFairy(concept: T) = Unit
    suspend fun onCancelled(concept: T, reason: CancellationReason) = Unit
}

class RuntimeContext(val playerSubunit: PlayerSubunits)

class BuildingCreateAct(private val runtimeContext: RuntimeContext) : StageAct<BuildingCreateConcept> {
    override val name: String = "BuildingCreate"

    override suspend fun createId(concept: BuildingCreateConcept): String {
        // BC-playerId-outpost1
        return "BC-${concept.playerId}-${concept.buildingId}"
    }

    override suspend fun createIdentity(concept: BuildingCreateConcept): Map<String, String> {
        return mapOf("buildingId" to concept.buildingId)
    }

    override suspend fun createSetup(concept: BuildingCreateConcept): ActSetup {
        return ActSetup(
            initialDelay = concept.buildDuration.toLong(DurationUnit.SECONDS),
            performMode = PerformMode.Once,
            lifetimeMode = LifetimeMode.Bound
        )
    }

    override suspend fun perform(concept: BuildingCreateConcept, times: Int) {
        println("Building ${concept.buildingId} created")
    }
}

class BuildingCreateConcept(
    val playerId: String,
    val buildingId: String,
    val buildDuration: Duration,
    val connection: Connection
) : ActConcept

@Serializable
class Photocard(
    val actId: String,
    val name: String,
    val progress: ActProgress,
    val metadata: Map<String, String>
)

data class ActSetup(
    val initialDelay: Long,
    val performMode: PerformMode,
    val lifetimeMode: LifetimeMode,
)

sealed class PerformMode {
    data object Once : PerformMode()
    data class Repeat(val times: Int, val interval: Long) : PerformMode()
    data class Forever(val interval: Long) : PerformMode()
}

sealed class LifetimeMode {
    object Bound : LifetimeMode()
    data class Persistent(val missedExecutionPolicy: MissedExecutionPolicy) : LifetimeMode()
}

sealed class MissedExecutionPolicy(val maxBatch: Int) {
    data object Skip : MissedExecutionPolicy(maxBatch = 0)
    data object LastOnly : MissedExecutionPolicy(maxBatch = 1)
    data class CatchUp(private val maxTimes: Int) : MissedExecutionPolicy(maxBatch = maxTimes)
}

fun x() {


}

/*
initialDelay     = 5s
repeatInterval = 10s
repetition     = 3

startedAt        : S
accumulatedDelay     : T
performCount          : C

0      5    9      15         25         35
S -----!----P------!----------!----------!
    5  !  4    6   !    10    !    10    !

S = 0
E = 35 (5 + 10 + 10 + 10)
L = 9 (5 + 4)
C = 1

firstExecutionAt   = startedAt + initialDelay
nextExecutionIndex = performCount + 1
nextExecutionAt    = firstExecutionAt + (nextExecutionIndex - 1) * repeatInterval
remainingDelay     = nextExecutionAt - accumulatedDelay
expectedPerformCount    = (now - firstExecutionAt) / repeatInterval + 1
missedPerformCount      = expectedPerformCount - performCount
remainingPerformCount   = repetition - performCount
actualRunsTodo     = min(missedperformCount, remainingPerformCount)
 */

@Serializable
class ActProgress(
    val startedAt: Long,
    val accumulatedDelay: Long,
    val performCount: Int
)

interface ActConcept

data class BuildingActConcept(
    val playerId: String,
    val buildingId: String,
    val connection: Connection
) : ActConcept


fun play() {

}

interface PhotocardRepository : Repository {
    suspend fun getAllPhotocards(playerId: String): Result<List<Photocard>>
}

fun Any.name(): String {
    return this::class.simpleName ?: "noname"
}

@Serializable
data class SavedAct(
    val playerId: String,
    val photocards: List<Photocard>
)


@Serializable
data class TestServerObjects(
    val dbId: String = ServerObjectsId,
    val acts: List<SavedAct>
)

class MongoPhotocardRepository(private val objects: MongoCollection<TestServerObjects>) : PhotocardRepository {
    override val name: String = "MongoPhotocardRepository"

    override suspend fun getAllPhotocards(playerId: String): Result<List<Photocard>> {
        return runMongoCatching {
            val doc = objects.find(
                Filters.and(
                    ServerObjectsFilter,
                    Filters.eq("acts.playerId", playerId)
                )
            ).projection(
                Projections.elemMatch("acts", Filters.eq(FieldPlayerId, playerId))
            ).firstOrNull()

            val act = doc?.acts?.singleOrNull { it.playerId == playerId }
            act?.photocards ?: emptyList()
        }
    }
}

class PhotocardSubunit(private val photocardRepository: PhotocardRepository) {
    suspend fun getAllPhotocards(playerId: String): List<Photocard> {
        return photocardRepository.getAllPhotocards(playerId)
            .onFailure {
                Fancam.error { "Failed to get all photocards for $playerId" }
            }
            .getOrNull() ?: emptyList()
    }
}

data class PhotocardFilter(
    val name: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    override fun toString(): String {
        return "$name(${metadata.entries.joinToString { "${it.key}=${it.value}" }})"
    }
}


