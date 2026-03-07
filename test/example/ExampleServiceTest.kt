package example

import CHANGE_ME_TEST_DB_NAME
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import core.PlayerService
import data.runMongoCatching
import data.throwIfNotModified
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.bson.Document
import utils.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Demonstrate the way to test services class and its integration with repository class.
 */
class ExampleServiceTest {
    /**
     * Principle:
     * - Focus on testing services class, test most of the methods that contains
     *   domain and processing logic; the repository can be mocked.
     *   Mock the repository as needed, may purposedly return success/fail;
     *   unused methods in the unit test can be ignored.
     * - Repository doesn't need to be tested.
     * - Can test repository through service + repository integration test.
     *   Though, would only test simple operations just to ensure its integration
     *   and not duplicating tests.
     */
    @Test
    fun testServices() = runTest {
        val mockRepo = object : ExampleRepository {
            override suspend fun getStrData(playerId: String) = Result.success("s1")
            override suspend fun getIntData(playerId: String) = Result.success(1)
            override suspend fun getOneFromManyStrData(playerId: String, s: String) = Result.success("s2")
            override suspend fun getAllStrData(playerId: String) = Result.success(listOf("s3", "s4"))
            override suspend fun updateStrData(playerId: String, newStrData: String): Result<Unit> {
                TODO()
            }

            override suspend fun updateIntData(playerId: String, newIntData: Int) = Result.success(Unit)
            override suspend fun updateOneFromManyStrData(
                playerId: String,
                oldOneStrData: String,
                newOneStrData: String
            ) = TODO()

            override suspend fun updateAllStrData(playerId: String, newManyStrData: List<String>) = TODO()
        }
        // ExampleService doesn't have complicated logic, so realistically no need to be tested.
        // but here we try to test init just for demonstration
        val service = ExampleService(mockRepo)


        val result = service.init("123")
        // ex. if all DB operation returned success, internal initialization shouldn't fail
        assert(result.isSuccess)
        // ex. get methods work correctly (in complex scenario, this may involve processing)
        assertEquals("s1", service.getStrData())
        // ex. updating should work, and service own data is also updated
        val result2 = service.updateIntData(2)
        assert(result2.isSuccess)
        assertEquals(2, service.getIntData())
        // here, we don't need to call getIntData() from DB
        // because we are not trying to DB's code
    }

    /**
     * As of now, integration test require real mongo.
     * Operations will also update real DB (result in disk increase).
     * Though, each test will delete artifacts.
     */
    @Test
    fun testServiceIntegration() = runTest {
        // must start real mongo
        val db = initMongo()

        // reset collection
        val collection = db.getCollection<ExampleModel>("excollection")
        collection.drop()
        db.createCollection("excollection")

        // insert base data
        val baseData = ExampleModel(
            playerId = "pid123",
            strData = "hello",
            intData = 42,
            manyStrData = listOf("a", "b", "c")
        )
        collection.insertOne(baseData)

        val repo = ExampleRepositoryMongo(collection)
        val service = ExampleService(repo)

        // init and ensure service initialization correctness
        service.init("pid123")
        assertEquals(42, service.getIntData())

        // try using the service to update data
        val result = service.updateStrData("updated")
        assertTrue(result.isSuccess)

        // ensure DB is updated, and match the data in service
        val updated = collection.find().first()
        assertEquals("updated", updated.strData)
        assertEquals("updated", service.getStrData())

        collection.drop()
    }

    private suspend fun initMongo(): MongoDatabase {
        val mongoc = MongoClient.create("mongodb://localhost:27017")
        val db = mongoc.getDatabase(CHANGE_ME_TEST_DB_NAME)
        db.runCommand(Document("ping", 1))
        return db
    }
}

data class ExampleModel(
    val playerId: String,
    val strData: String,
    val intData: Int,
    val manyStrData: List<String>
)

/**
 * Example of a repository.
 *
 * All functions are marked as suspend to be able to work with coroutine.
 *
 * It is preferred that any operation always return a result type.
 * Use the return type as `Result<T>` or `Unit` if it doesn't have a return type.
 */
interface ExampleRepository {
    suspend fun getStrData(playerId: String): Result<String>
    suspend fun getIntData(playerId: String): Result<Int>
    suspend fun getOneFromManyStrData(playerId: String, s: String): Result<String>
    suspend fun getAllStrData(playerId: String): Result<List<String>>

    suspend fun updateStrData(playerId: String, newStrData: String): Result<Unit>
    suspend fun updateIntData(playerId: String, newIntData: Int): Result<Unit>
    suspend fun updateOneFromManyStrData(playerId: String, oldOneStrData: String, newOneStrData: String): Result<Unit>
    suspend fun updateAllStrData(playerId: String, newManyStrData: List<String>): Result<Unit>
}

/**
 * [ExampleRepository] implementation with MongoDB.
 */
class ExampleRepositoryMongo(val data: MongoCollection<ExampleModel>) : ExampleRepository {
    override suspend fun getStrData(playerId: String): Result<String> {
        return runMongoCatching {
            val filter = Filters.eq("playerId", playerId)
            data.find(filter)
                .firstOrNull()
                ?.strData
        }
    }

    override suspend fun getIntData(playerId: String): Result<Int> {
        return runMongoCatching {
            val filter = Filters.eq("playerId", playerId)
            data.find(filter)
                .firstOrNull()
                ?.intData
        }
    }

    override suspend fun getOneFromManyStrData(
        playerId: String,
        s: String
    ): Result<String> {
        return runMongoCatching {
            val filter = Filters.eq("playerId", playerId)
            data.find(filter)
                .firstOrNull()
                ?.manyStrData
                ?.find { it == s }
        }
    }

    override suspend fun getAllStrData(playerId: String): Result<List<String>> {
        return runMongoCatching {
            val filter = Filters.eq("playerId", playerId)
            data.find(filter)
                .firstOrNull()
                ?.manyStrData
        }
    }

    override suspend fun updateStrData(playerId: String, newStrData: String): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.eq("playerId", playerId)
            val update = Updates.set("strData", newStrData)

            val result = data.updateOne(filter, update)
            result.throwIfNotModified(playerId)
        }
    }

    override suspend fun updateIntData(playerId: String, newIntData: Int): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.eq("playerId", playerId)
            val update = Updates.set("intData", newIntData)

            val result = data.updateOne(filter, update)
            result.throwIfNotModified(playerId)
        }
    }

    override suspend fun updateOneFromManyStrData(
        playerId: String,
        oldOneStrData: String,
        newOneStrData: String
    ): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.and(
                Filters.eq("playerId", playerId),
                Filters.eq("manyStrData", oldOneStrData),
            )
            val update = Updates.set("manyStrData.$", newOneStrData)

            val result = data.updateOne(filter, update)
            result.throwIfNotModified(playerId)
        }
    }

    override suspend fun updateAllStrData(
        playerId: String,
        newManyStrData: List<String>
    ): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.eq("playerId", playerId)
            val update = Updates.set("manyStrData", newManyStrData)

            val result = data.updateOne(filter, update)
            result.throwIfNotModified(playerId)
        }
    }
}

/**
 * Example service class holding a repository contract.
 */
class ExampleService(private val exampleRepository: ExampleRepository) : PlayerService {
    private lateinit var playerId: String
    private var strData: String = ""
    private var intData: Int = 0
    private val manyStrData = mutableListOf<String>()

    fun getStrData() = strData
    fun getIntData() = intData
    fun getManyStrData() = manyStrData

    fun getOneManyStrData(s: String): Result<String> {
        val s = manyStrData.find { it == s }
        return if (s != null) {
            Result.success(s)
        } else {
            Result.failure(NoSuchElementException("String $s wasn't found."))
        }
    }

    suspend fun updateStrData(s: String): Result<Unit> {
        val result = exampleRepository.updateStrData(playerId, s)
        result.onFailure {
            Logger.error { "Error on ExampleService-updateStrData: ${it.message}" }
        }
        result.onSuccess {
            strData = s
        }
        return result
    }

    suspend fun updateIntData(i: Int): Result<Unit> {
        val result = exampleRepository.updateIntData(playerId, i)
        result.onFailure {
            Logger.error { "Error on ExampleService-updateIntData: ${it.message}" }
        }
        result.onSuccess {
            intData = i
        }
        return result
    }

    suspend fun updateOneManyStrData(old: String, new: String): Result<Unit> {
        val result = exampleRepository.updateOneFromManyStrData(playerId, old, new)
        result.onFailure {
            Logger.error { "Error on ExampleService-updateOneManyStrData: ${it.message}" }
        }
        result.onSuccess {
            manyStrData.removeIf { it == old }
            manyStrData.add(new)
        }
        return result
    }

    suspend fun updateAllManyStrData(m: List<String>): Result<Unit> {
        val result = exampleRepository.updateAllStrData(playerId, m)
        result.onFailure {
            Logger.error { "Error on ExampleService-updateAllManyStrData: ${it.message}" }
        }
        result.onSuccess {
            manyStrData.clear()
            manyStrData.addAll(m)
        }
        return result
    }

    override suspend fun init(playerId: String): Result<Unit> {
        return runCatching {
            this.playerId = playerId
            this.strData = exampleRepository.getStrData(playerId).getOrThrow()
            this.intData = exampleRepository.getIntData(playerId).getOrThrow()
            this.manyStrData.addAll(exampleRepository.getAllStrData(playerId).getOrThrow())
        }
    }

    override suspend fun close(playerId: String): Result<Unit> = Result.success(Unit)
}
