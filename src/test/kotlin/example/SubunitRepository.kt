package example

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import encore.datastore.DocumentNotFoundException
import encore.datastore.runMongoCatching
import encore.datastore.throwIfNotModified
import encore.fancam.Fancam
import encore.subunit.Subunit
import encore.subunit.helper.failHandleGet
import encore.subunit.helper.failHandleUpdate
import encore.subunit.scope.PlayerScope
import encore.utils.Report
import encore.utils.isOk
import encore.utils.okOrNull
import encore.utils.toReport
import encore.utils.toReportOkOrFail
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import testHelper.initMongo
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * This file demonstrate a full example of handler-subunit-repository integration.
 *
 * This test gives example of how to test a subunit.
 */
@Ignore
class ExampleSubunitTest {
    /**
     * Principle:
     * - Repository is typically not tested since it only contains call to the
     *   underlying database and domain logic is operated on subunit.
     * - Test subunits operation that contains domain and processing logic.
     *   A simple get methods that depends on repository solely without additional
     *   logic may not need to be tested.
     * - Repository should be mocked, purposedly return success/fail.
     * - Repository may be tested by the integration test of subunit and repository.
     *   Though, would only test simple operations just to ensure its integration
     *   and not duplicating tests.
     */
    @Test
    fun testSubunits() = runTest {
        val mockRepo = object : PlayerRepository {
            override suspend fun getRole(playerId: String) = Result.success("s1")
            override suspend fun getDamage(playerId: String) = Result.success(1)
            override suspend fun getItem(playerId: String, item: String) = Result.success("s2")
            override suspend fun getAllItems(playerId: String) = Result.success(listOf("s3", "s4"))
            override suspend fun updateRole(playerId: String, newRole: String): Result<Unit> {
                TODO()
            }

            override suspend fun updateDamage(playerId: String, newDamage: Int) = Result.success(Unit)
            override suspend fun updateItem(
                playerId: String,
                oldItem: String,
                newItem: String
            ) = TODO()

            override suspend fun updateAllItems(playerId: String, newItems: List<String>) = TODO()
        }

        val subunit = PlayerSubunit(mockRepo)

        // ex. get methods work correctly (in complex scenario, this may involve processing)
        assertEquals("s1", subunit.getRole().okOrNull())

        // ex. updating should work, and subunit own's data is also updated
        val report = subunit.updateDamage(2)
        assert(report.isOk())
        assertEquals(2, subunit.getDamage().okOrNull())
    }

    /**
     * Integration test which require real mongo.
     */
    @Test
    fun testSubunitIntegration() = runTest {
        val db = initMongo()

        // reset collection
        val collection = db.getCollection<PlayerModel>("ex_subunit_col")
        collection.drop()
        db.createCollection("ex_subunit_col")

        // insert base data
        val baseData = PlayerModel(
            playerId = "pid123",
            role = "hello",
            damage = 42,
            items = listOf("a", "b", "c")
        )
        collection.insertOne(baseData)

        val repo = MongoPlayerRepository(collection)
        val subunit = PlayerSubunit(repo)

        // ex. ensure initialization
        val result = subunit.debut(PlayerScope("pid123"))
        assert(result.isSuccess)

        // ex. ensure get
        assertEquals(42, subunit.getDamage().okOrNull())

        // ex. ensure update
        val report = subunit.updateRole("updated")
        assertTrue(report.isOk())

        // ex. ensure DB is updated and match the data in subunit
        val updated = collection.find().first()
        assertEquals("updated", updated.role)
        assertEquals("updated", subunit.getRole().okOrNull())

        collection.drop()
    }
}

/**
 * Example of a domain model
 */
data class PlayerModel(
    val playerId: String,
    val role: String,
    val damage: Int,
    val items: List<String>
)

/**
 * Example of a repository handling PlayerModel.
 *
 * Practically though, a repository should handle a single concern (e.g., ItemsRepository)
 * rather than aggregate (unless they are deeply related).
 *
 * All functions are suspend for native coroutine support.
 *
 * Each operation should return a value wrapped in a result type.
 * A `Result<Unit>` can be used when it doesn't have a return type.
 */
interface PlayerRepository {
    suspend fun getRole(playerId: String): Result<String>
    suspend fun getDamage(playerId: String): Result<Int>
    suspend fun getItem(playerId: String, item: String): Result<String>
    suspend fun getAllItems(playerId: String): Result<List<String>>

    suspend fun updateRole(playerId: String, newRole: String): Result<Unit>
    suspend fun updateDamage(playerId: String, newDamage: Int): Result<Unit>
    suspend fun updateItem(playerId: String, oldItem: String, newItem: String): Result<Unit>
    suspend fun updateAllItems(playerId: String, newItems: List<String>): Result<Unit>
}

/**
 * Example of a repository implementation using MongoDB.
 *
 * **Note: Example here tries to introduces strictness. Actual code may slightly lower
 * the rigor when necessary.**
 *
 * ### Constructor
 *
 * MongoDB operations work through `MongoCollection`, therefore the initialization
 * of this repository would depend on `MongoClient` or `MongoDatabase`.
 *
 * ### Exception Handling in Operations
 *
 * Typically, operation is wrapped with [runMongoCatching], allowing
 * any errors to be caught and wrapped into the `Result` type.
 *
 * Typically, the result of update, replace, and delete operations are
 * checked to ensure a modification was done. Helper such as [throwIfNotModified]
 * can be used easily check the result and throw an error.
 *
 * ### Exception Logging
 *
 * No logging should be done in the repository layer, though repository
 * may attach context by passing a string message into `runMongoCatching`.
 *
 * Error that happens in the repository layer is usually Mongo's internal
 * or when the document doesn't exist (which already have a default error message).
 * This makes custom message rarely used.
 *
 * Error would be propagated to the subunit layer where detailed domain
 * logging should be done. This makes error shows up like:
 * - Suppose that the `items` for playerABC does not exist.
 * - By using `runMongoCatching`, getting a `null` data will throw [DocumentNotFoundException]
 *   which has a default message of "Expected document not exist".
 * - The exception will be wrapped in a [Result.failure].
 * - The result is passed to the subunit layer.
 * - Subunit checks whether the result fails and handles accordingly.
 * - It may log something like "Items not found for playerABC".
 *
 * This results in clear separation and no duplicate error logging.
 */
class MongoPlayerRepository(val data: MongoCollection<PlayerModel>) : PlayerRepository {
    override suspend fun getRole(playerId: String): Result<String> {
        return runMongoCatching {
            val filter = Filters.eq("playerId", playerId)
            data.find(filter)
                .firstOrNull()
                ?.role
        }
    }

    override suspend fun getDamage(playerId: String): Result<Int> {
        return runMongoCatching {
            val filter = Filters.eq("playerId", playerId)
            data.find(filter)
                .firstOrNull()
                ?.damage
        }
    }

    override suspend fun getItem(playerId: String, item: String): Result<String> {
        return runMongoCatching {
            val filter = Filters.eq("playerId", playerId)
            data.find(filter)
                .firstOrNull()
                ?.items
                ?.find { it == item }
        }
    }

    override suspend fun getAllItems(playerId: String): Result<List<String>> {
        return runMongoCatching {
            val filter = Filters.eq("playerId", playerId)
            data.find(filter)
                .firstOrNull()
                ?.items
        }
    }

    override suspend fun updateRole(playerId: String, newRole: String): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.eq("playerId", playerId)
            val update = Updates.set("role", newRole)

            data.updateOne(filter, update)
                .throwIfNotModified(playerId)
        }
    }

    override suspend fun updateDamage(playerId: String, newDamage: Int): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.eq("playerId", playerId)
            val update = Updates.set("damage", newDamage)

            data.updateOne(filter, update)
                .throwIfNotModified(playerId)
        }
    }

    override suspend fun updateItem(playerId: String, oldItem: String, newItem: String): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.and(
                Filters.eq("playerId", playerId),
                Filters.eq("items", oldItem)
            )
            val update = Updates.set("items.$", newItem)

            data.updateOne(filter, update)
                .throwIfNotModified(playerId)
        }
    }

    override suspend fun updateAllItems(playerId: String, newItems: List<String>): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.eq("playerId", playerId)
            val update = Updates.set("items", newItems)

            data.updateOne(filter, update)
                .throwIfNotModified(playerId)
        }
    }
}

/**
 * Example of a subunit scoped to a player that carries [PlayerRepository].
 * Acts as a domain-level abstraction for higher-level components such as message handlers.
 *
 * **Note: Example here tries to introduces strictness. Actual code may slightly lower
 * the rigor when necessary.**
 *
 * ### Local Data & Initialization
 *
 * Data is cached locally to minimize repeated get access to the repository.
 * Update operations modify both the local cache and the repository.
 *
 * All fields are initialized as null or empty collections, and populated in [debut].
 * Initialization failures are logged immediately but do not interrupt the subunit
 * or the player's connection (fail-late).
 *
 * Subsequent access to uninitialized or missing data will fail-fast at usage sites
 * (e.g., handlers may return early or ignore the request). Handlers are not responsible
 * for logging these failures, as they are already handled and logged within the subunit.
 *
 * Example: if [items] fails to load during initialization, the error is logged at startup.
 * The player's session continues, but any access to [items] may fail at runtime.
 *
 * ### Subunit API
 *
 * The subunit exposes domain-oriented APIs derived from repository operations.
 * These APIs may extend or refine repository data into more meaningful abstractions.
 *
 * Example: the repository provides `getInventory()`, while the subunit may expose
 * higher-level queries such as `getEquipment()` or `getFood()` by filtering the data.
 *
 * ### Logging
 *
 * The subunit is responsible for handling [Result] from the repository.
 * Typically uses [toReport] along with helpers like [failHandleGet] and [failHandleUpdate].
 * Both fail handle helpers has a default message that can be used to avoid writing logging
 * boilerplate, in the case where context is clear enough.
 *
 * - On success: may log at trace level for detailed debugging.
 * - On failure: logs in domain language, avoiding repository/DB-specific details.
 *
 * ### Return Types
 *
 * All public operations return [Report], representing domain-level outcomes:
 * - [Report.Ok] for success (with value)
 * - [Report.Fail] for failure (no exception exposed)
 *
 * Since initialization is fail-late and fields may remain null, all `get` operations
 * also return [Report]. Helpers like [toReportOkOrFail] can be used to convert nullable
 * values into [Report].
 *
 * Typically, no additional logging is needed in `get` methods, as initialization failures
 * are already logged during [debut].
 *
 * ### Handler Example
 *
 * ```
 * // request remain unhandled
 * val role = playerSubunit.getRole().okOrNull() ?: return
 *
 * // runtime error thrown
 * val damage = playerSubunit.getDamage().require { "Damage was null" }
 *
 * // manual handling of report
 * val items = playerSubunit.getItems()
 * val response = subunit.getItems().handles(
 *      onOk = { items -> ResponseSuccess(items) },
 *      onFail = {
 *          Fancam.trace { "Player data request fail during getItems()" }
 *          ResponseFailed()
 *      }
 *  )
 *  connection.send(response)
 * ```
 */
class PlayerSubunit(private val playerRepository: PlayerRepository) : Subunit<PlayerScope> {
    private lateinit var playerId: String
    private var role: String? = null
    private var damage: Int? = null
    private val items = mutableListOf<String>()

    fun getRole(): Report<String> = role.toReportOkOrFail()
    fun getDamage(): Report<Int> = damage.toReportOkOrFail()
    fun getItems(): Report<List<String>> = items.toReportOkOrFail()
    fun getItem(item: String): Report<String> = items.find { it == item }.toReportOkOrFail()

    suspend fun updateRole(role: String): Report<Unit> {
        val result = playerRepository.updateRole(playerId, role)

        return result.toReport(
            onSuccess = {
                this.role = role
                Fancam.trace { "OK updateRole with role=$role" }
            },
            onFailure = { it.failHandleUpdate() }
        )
    }

    suspend fun updateDamage(damage: Int): Report<Unit> {
        val result = playerRepository.updateDamage(playerId, damage)

        return result.toReport(
            onSuccess = {
                this.damage = damage
                Fancam.trace { "OK updateDamage with damage=$damage" }
            },
            onFailure = { it.failHandleUpdate() }
        )
    }

    suspend fun updateItem(oldItem: String, newItem: String): Report<Unit> {
        val result = playerRepository.updateItem(playerId, oldItem, newItem)

        return result.toReport(
            onSuccess = {
                this.items.removeIf { it == oldItem }
                this.items.add(newItem)
                Fancam.trace { "OK updateItem with $oldItem to $newItem" }
            },
            onFailure = {
                it.failHandleUpdate(
                    notFoundMessage = { "Couldn't update $oldItem to $newItem because $oldItem wasn't found." },
                    notUpdatedMessage = { "Couldn't update $oldItem to $newItem update didn't affected" },
                    unknownMessage = { "Couldn't update $oldItem to $newItem with unknown error" },
                )
            }
        )
    }

    suspend fun updateAllItems(newItems: List<String>): Report<Unit> {
        val result = playerRepository.updateAllItems(playerId, newItems)

        return result.toReport(
            onSuccess = {
                this.items.clear()
                this.items.addAll(newItems)
                Fancam.trace { "OK updateAllItems to ${newItems.joinToString()}" }
            },
            onFailure = { it.failHandleUpdate() }
        )
    }

    override suspend fun debut(scope: PlayerScope): Result<Unit> {
        return runCatching {
            this.playerId = scope.playerId
            playerRepository.getRole(scope.playerId).fold(
                onSuccess = { this.role = it },
                onFailure = { it.failHandleGet() }
            )
            playerRepository.getDamage(scope.playerId).fold(
                onSuccess = { this.damage = it },
                onFailure = { it.failHandleGet() }
            )
            playerRepository.getAllItems(scope.playerId).fold(
                onSuccess = { this.items.addAll(it) },
                onFailure = { it.failHandleGet() }
            )
        }
    }

    override suspend fun disband(scope: PlayerScope): Result<Unit> = Result.success(Unit)
}
