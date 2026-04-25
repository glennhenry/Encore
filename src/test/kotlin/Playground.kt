import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.kotlin.client.coroutine.MongoCollection
import encore.context.PlayerSubunits
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
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.DurationUnit
import encore.datastore.collection.PlayerId
import encore.datastore.collection.ServerObjects
import encore.subunit.Subunit
import encore.subunit.scope.ServerScope
import encore.tasks.ServerTaskDispatcher

/**
 * Playground for quick testing and code run
 *
 * .\gradlew test --tests "Playground.playground" --console=plain
 */
class Playground {
    @Test
    fun playground() {
        for (i in 0 until 257) {
            println("\u001B[48;5;${i}m THIS IS $i")

        }
    }
}


/**
 * Represents a scheduled server-side task.
 *
 * A `StageAct` is a deferred execution bound to a runtime owner (e.g., a player or the server)
 * with a defined lifecycle. It takes an [ActConcept] as input for execution.
 *
 * Stage acts are typically used for time-based or repeated operations. For example,
 * a building construction that completes after 5 minutes can be modeled as a `StageAct`
 * with a 5 minutes delay, and will notify player of its completion inside [perform].
 *
 * #### Identification
 *
 * Each stage act must define a unique identifier via [createId]. This ID must be globally
 * unique across all stage act instances. It is typically derived from domain-specific
 * identifiers (e.g., `playerId`, `buildingId`).
 *
 * [createIdentity] provides additional key-value data to distinguish multiple instances
 * of the same `StageAct` type. For example, multiple construction tasks may share the
 * same type but differ by `buildingId` and `buildDuration`.
 *
 * #### Configuration
 *
 * The execution behavior is defined by [createSetup], producing an [ActSetup] including
 * `initialDelay`, [PerformMode], and [LifetimeMode].
 *
 * #### Lifecycle
 *
 * The stage act system offers 4 lifecycles:
 * - [onStart]: Invoked once when the act is first scheduled to run
 *              (e.g., by calling [ServerTaskDispatcher.runTask]).
 * - [perform]: The main execution step. Invoked once or repeatedly depending on [PerformMode].
 * - [onEndingFairy]: Invoked once when the act completes all scheduled executions.
 *     - [PerformMode.Once]: called immediately after [perform]
 *     - [PerformMode.Repeat]: called after the final [perform]
 *     - [PerformMode.Forever]: never called
 * - [onCancelled]: Invoked if the act is cancelled before normal completion
(e.g., via [ServerTaskDispatcher.stopTask]).
 *
 * @param T The type of [ActConcept] for this stage act.
 */
interface StageAct<T : ActConcept> {
    /**
     * The name of this `StageAct`, used for debugging and identification on [Photocard.name].
     */
    val name: String

    /**
     * Produces a globally unique identifier for this act instance.
     */
    suspend fun createId(concept: T): String

    /**
     * Produces additional identity data for distinguishing instances of the same act type.
     */
    suspend fun createIdentity(concept: T): Map<String, String> = emptyMap()

    /**
     * Produces the execution configuration for this act.
     */
    suspend fun createSetup(concept: T): ActSetup

    /**
     * Called once when the act is first scheduled.
     * Use for initialization or side effects (e.g., notifying a client).
     */
    suspend fun onStart(concept: T) = Unit

    /**
     * Main execution body of the act.
     *
     * [times] represents the batch size for the current [perform] call.
     *
     * For example, if 6 executions were missed and a [MissedPerformPolicy.CatchUp] is applied,
     * `times` will be 6. Under normal conditions (no missed executions), `times` is typically 1.
     *
     * Stage act should implement `perform` in inclusion of the arbitrary amount of [times],
     * unless [MissedPerformPolicy] is specifically skip or last only.
     *
     * @param times Number of executions to process in a single batch.
     */
    suspend fun perform(concept: T, times: Int)

    /**
     * Called once when the act completes all scheduled executions successfully.
     */
    suspend fun onEndingFairy(concept: T) = Unit

    /**
     * Called if the act is cancelled before completing normally.
     *
     * @param reason The cause of cancellation.
     */
    suspend fun onCancelled(concept: T, reason: CancellationReason) = Unit
}

/**
 * Describe the reason of why a [StageAct] is cancelled.
 */
enum class CancellationReason {
    /**
     * The normal path of explicit cancellation.
     */
    Manual,

    /**
     * The act is cancelled because the player is disconnected.
     */
    Disconnect,

    /**
     * The act is cancelled due to some error during it's execution.
     */
    Error
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
    val playerId: PlayerId,
    val buildingId: String,
    val buildDuration: Duration,
    val connection: Connection
) : ActConcept

/**
 * Represents a snapshot of an unfinished [StageAct].
 *
 * Stage acts with [LifetimeMode.Persistent] that are not completed when a player
 * disconnects are serialized and stored in the database as a `Photocard`.
 *
 * This model contains only the minimal information required to restore and resume
 * the act at a later time.
 *
 * @property actId Unique identifier of this stage act.
 * @property name Identifier of the associated stage act (typically [StageAct.name]).
 * @property progress Current progress state of the stage act.
 * @property data Key-value data used to distinguish multiple acts of the same name.
 */
@Serializable
class Photocard(
    val actId: String,
    val name: String,
    val progress: ActProgress,
    val data: Map<String, String>
)

/**
 * Runtime configuration model of [StageAct].
 *
 * [ActSetup] describes the execution detail of a stage act.
 *
 * @property initialDelay The amount of delay carried out before the act
 *                        performs for the first time.
 * @property performMode Defines the execution portion.
 * @property lifetimeMode Defines the act's existence.
 */
data class ActSetup(
    val initialDelay: Long,
    val performMode: PerformMode,
    val lifetimeMode: LifetimeMode,
)

/**
 * Describes [StageAct] performs portion.
 *
 * - [PerformMode.Once]
 * - [PerformMode.Repeat]
 * - [PerformMode.Forever]
 */
sealed class PerformMode {
    /**
     * The stage act is performed exactly once.
     */
    data object Once : PerformMode()

    /**
     * The stage act is performed repeatedly with a fixed interval.
     *
     * The total number of executions is `times + 1`, as [times] counts only
     * the additional repeats after the initial execution.
     *
     * Examples:
     * - `times = 0` is equivalent to [Once]
     * - `times = 1` is executed twice in total
     *
     * @property times Number of additional executions after the first run.
     * @property interval Interval between executions, in milliseconds.
     */
    data class Repeat(val times: Int, val interval: Long) : PerformMode()

    /**
     * The stage act is performed indefinitely at a fixed interval.
     *
     * @property interval Interval between executions, in milliseconds.
     */
    data class Forever(val interval: Long) : PerformMode()
}

/**
 * Defines the lifetime behavior of a [StageAct].
 *
 * - [LifetimeMode.Bound] represents temporary acts tied to some runtime owner
 *   (e.g., a player's connection). The act is terminated when its owner is no longer valid,
 *   even if it has not completed.
 * - [LifetimeMode.Persistent] represents durable acts that are stored and can be
 *   resumed later if they have not yet completed.
 */
sealed class LifetimeMode {
    /**
     * A temporary act bound to a runtime owner.
     */
    object Bound : LifetimeMode()

    /**
     * A persistent act that survives runtime bounds and can be resumed.
     *
     * @property missedPerformPolicy Execution policy applied when the act is resumed
     *                               if one or more executions are missed.
     */
    data class Persistent(val missedPerformPolicy: MissedPerformPolicy) : LifetimeMode()
}

/**
 * Defines how a [StageAct] handles missed executions while inactive.
 *
 * Stage acts execute continuously while active. For persistable acts, any executions
 * missed during inactivity are resolved according to this policy upon resumption.
 *
 * @property maxBatch Maximum number of missed executions to process in a single [StageAct.perform] call.
 */
sealed class MissedPerformPolicy(val maxBatch: Int) {
    /**
     * Discards all missed executions (`maxBatch = 0`).
     */
    data object Skip : MissedPerformPolicy(maxBatch = 0)

    /**
     * Executes only the most recent missed execution (`maxBatch = 1`).
     */
    data object LastOnly : MissedPerformPolicy(maxBatch = 1)

    /**
     * Executes every missed executions up to a defined limit.
     *
     * @property maxTimes Maximum number of executions to process when catching up.
     *                   This should be bounded to prevent excessive workload unless
     *                   the [StageAct] implementation already handles it safely.
     */
    data class CatchUp(private val maxTimes: Int) : MissedPerformPolicy(maxBatch = maxTimes)
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

/**
 * Represent the progress state of a [StageAct].
 *
 * @property startedAt Epoch millis of when the act was scheduled to run
 *                     (i.e., via [ServerTaskDispatcher.runTask]).
 * @property accumulatedDelay The accumulation of delay taken whilst running the act,
 *                            which includes the [ActSetup.initialDelay] and each repetition's
 *                            interval if the act is repeatable. This does not include the
 *                            act's execution time.
 * @property performCount The total amount of times [StageAct.perform] has been called.
 */
@Serializable
class ActProgress(
    val startedAt: Long,
    val accumulatedDelay: Long,
    val performCount: Int
)


/**
 * Marker interface representing the input for a stage act.
 *
 * An `ActConcept` encapsulates all data required to execute a [StageAct].
 * This includes both static and runtime data, but excludes external dependencies.
 *
 * For instance, a building construction task for a player may include:
 * - [PlayerId] of the player
 * - `buildingId` identifiying the building being constructed.
 * - `buildDuration` defining the construction time, which is also used to determine
 *   the act's delay in [ActSetup].
 * - [Connection] object used to send a building constructed message.
 *
 * External dependencies (e.g., `BuildingSubunit`) should not be part of the concept
 * and must instead be injected into the [StageAct] implementation.
 *
 * Each [StageAct] has a corresponding `ActConcept`. The concept is used for
 * unique ID, identity, and [ActSetup] creation, and throughout the act lifecycle.
 *
 * All stage act input types should implement this interface.
 */
interface ActConcept


data class BuildingActConcept(
    val playerId: PlayerId,
    val buildingId: String,
    val connection: Connection
) : ActConcept


/**
 * Repository for [Photocard].
 *
 * Implementation provides access to the photocards of each [PlayerId].
 */
interface PhotocardRepository : Repository {
    /**
     * Get all photocards for [playerId]
     * @return Every photocards in [Result] type.
     * - [Result.failure] when there is an internal repo/DB error.
     * - [Result.success] otherwise, including case when photocards are empty.
     */
    suspend fun getAllPhotocards(playerId: PlayerId): Result<List<Photocard>>
}


/**
 * Holds all persistable [StageAct] of [playerId].
 *
 * @property playerId The associated player.
 * @property photocards Every photocards instance.
 */
@Serializable
data class SavedAct(
    val playerId: PlayerId,
    val photocards: List<Photocard>
)


@Serializable
data class TestServerObjects(
    val dbId: String = ServerObjectsId,
    val acts: List<SavedAct>
)

/**
 * Implementation of [PhotocardRepository] with Mongo.
 *
 * @property objects [ServerObjects] collection.
 */
class MongoPhotocardRepository(private val objects: MongoCollection<TestServerObjects>) : PhotocardRepository {
    override val name: String = "MongoPhotocardRepository"

    override suspend fun getAllPhotocards(playerId: PlayerId): Result<List<Photocard>> {
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

    override suspend fun debut(scope: ServerScope): Result<Unit> = Result.success(Unit)
    override suspend fun disband(scope: ServerScope): Result<Unit> = Result.success(Unit)
}
