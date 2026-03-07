package user

import CHANGE_ME_TEST_DB_NAME
import com.mongodb.assertions.Assertions.assertFalse
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import data.MongoImpl
import data.collection.PlayerAccount
import kotlinx.coroutines.test.runTest
import org.bson.Document
import user.auth.SessionManager
import user.auth.DefaultAuthProvider
import user.model.ServerMetadata
import user.model.UserProfile
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration test for [DefaultAuthProvider] and [PlayerAccountRepository].
 *
 * Ensure MongoDB is running.
 */
class TestDefaultAuthProvider {
    @Test
    fun `test doesUserExist for registered user return true`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<PlayerAccount>("player_account")
        collection.drop()
        mongoDb.createCollection("test_player_account")

        val db = MongoImpl(mongoDb, false)
        val manager = SessionManager()
        val repo = PlayerAccountRepositoryMongo(collection)
        val provider = DefaultAuthProvider(db, repo, manager)

        val account = PlayerAccount(
            playerId = "pid12345",
            hashedPassword = "hashed123",
            profile = UserProfile.default("pid12345", "name"),
            metadata = ServerMetadata()
        )
        collection.insertOne(account)

        assertTrue(provider.doesUsernameExist("name"))
        assertFalse(provider.isUsernameAvailable("name"))
    }

    @Test
    fun `test doesUserExist for unregistered user return true`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<PlayerAccount>("player_account")
        collection.drop()
        mongoDb.createCollection("test_player_account")

        val db = MongoImpl(mongoDb, false)
        val manager = SessionManager()
        val repo = PlayerAccountRepositoryMongo(collection)
        val provider = DefaultAuthProvider(db, repo, manager)

        assertFalse(provider.doesUsernameExist("xyz"))
        assertTrue(provider.isUsernameAvailable("xyz"))
    }

    @Test
    fun `test register successfully create user`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<PlayerAccount>("player_account")
        collection.drop()
        mongoDb.createCollection("test_player_account")

        val db = MongoImpl(mongoDb, false)
        val manager = SessionManager()
        val repo = PlayerAccountRepositoryMongo(collection)
        val provider = DefaultAuthProvider(db, repo, manager)

        provider.register("helloworld", "kotlinktor")
        assertTrue(provider.doesUsernameExist("helloworld"))
        assertFalse(provider.isUsernameAvailable("helloworld"))
    }

    @Test
    fun `test login but account don't exist return failure result`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<PlayerAccount>("player_account")
        collection.drop()
        mongoDb.createCollection("test_player_account")

        val db = MongoImpl(mongoDb, false)
        val manager = SessionManager()
        val repo = PlayerAccountRepositoryMongo(collection)
        val provider = DefaultAuthProvider(db, repo, manager)

        val session = provider.login("asdf", "fdsa")
        assertTrue(session.isFailure)
    }

    @Test
    fun `test login wrong credentials return null`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<PlayerAccount>("player_account")
        collection.drop()
        mongoDb.createCollection("test_player_account")

        val db = MongoImpl(mongoDb, false)
        val manager = SessionManager()
        val repo = PlayerAccountRepositoryMongo(collection)
        val provider = DefaultAuthProvider(db, repo, manager)

        provider.register("helloworld", "kotlinktor")
        val session = provider.login("helloworld", "ktor")
        assertTrue(session.isFailure)
    }

    @Test
    fun `test login user registered and correct credentials return non-null`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<PlayerAccount>("player_account")
        collection.drop()
        mongoDb.createCollection("test_player_account")

        val db = MongoImpl(mongoDb, false)
        val manager = SessionManager()
        val repo = PlayerAccountRepositoryMongo(collection)
        val provider = DefaultAuthProvider(db, repo, manager)

        provider.register("helloworld", "kotlinktor")
        assertNotNull(provider.login("helloworld", "kotlinktor"))
    }

    private suspend fun initMongo(): MongoDatabase {
        val mongoc = MongoClient.create("mongodb://localhost:27017")
        val db = mongoc.getDatabase(CHANGE_ME_TEST_DB_NAME)
        db.runCommand(Document("ping", 1))
        return db
    }
}
