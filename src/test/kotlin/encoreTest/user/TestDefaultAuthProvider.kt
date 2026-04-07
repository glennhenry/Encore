package encoreTest.user

import com.mongodb.assertions.Assertions.assertFalse
import encore.datastore.MongoDataStore
import encore.datastore.collection.PlayerAccount
import encore.user.PlayerAccountRepository
import encore.user.PlayerAccountRepositoryMongo
import encore.user.PlayerCreationSubunit
import encore.user.auth.DefaultAuthProvider
import encore.user.auth.SessionManager
import encore.user.model.ServerMetadata
import kotlinx.coroutines.test.runTest
import testHelper.TestMongoCollectionName
import testHelper.createProfile
import testHelper.initMongo
import kotlin.test.Test
import kotlin.test.assertNotNull
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
        val collection = mongoDb.getCollection<PlayerAccount>(TestMongoCollectionName.playerAccount)
        collection.drop()
        mongoDb.createCollection(TestMongoCollectionName.playerAccount)

        val db = MongoDataStore(mongoDb, TestMongoCollectionName)
        val manager = SessionManager()
        val repo = PlayerAccountRepositoryMongo(collection)
        val pcs = PlayerCreationSubunit(db)
        val provider = DefaultAuthProvider(pcs, repo, manager)

        val account = PlayerAccount(
            playerId = "pid12345",
            username = "name",
            email = "anyemail",
            hashedPassword = "anypassword",
            profile = createProfile("pid12345"),
            metadata = ServerMetadata()
        )
        collection.insertOne(account)

        assertTrue(provider.doesUsernameExist("name"))
        assertFalse(provider.isUsernameAvailable("name"))
    }

    @Test
    fun `test doesUserExist for unregistered user return true`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<PlayerAccount>(TestMongoCollectionName.playerAccount)
        collection.drop()
        mongoDb.createCollection(TestMongoCollectionName.playerAccount)

        val db = MongoDataStore(mongoDb, TestMongoCollectionName)
        val manager = SessionManager()
        val repo = PlayerAccountRepositoryMongo(collection)
        val pcs = PlayerCreationSubunit(db)
        val provider = DefaultAuthProvider(pcs, repo, manager)

        assertFalse(provider.doesUsernameExist("xyz"))
        assertTrue(provider.isUsernameAvailable("xyz"))
    }

    @Test
    fun `test register successfully create user`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<PlayerAccount>(TestMongoCollectionName.playerAccount)
        collection.drop()
        mongoDb.createCollection(TestMongoCollectionName.playerAccount)

        val db = MongoDataStore(mongoDb, TestMongoCollectionName)
        val manager = SessionManager()
        val repo = PlayerAccountRepositoryMongo(collection)
        val pcs = PlayerCreationSubunit(db)
        val provider = DefaultAuthProvider(pcs, repo, manager)

        provider.register("helloworld", "kotlinktor")
        assertTrue(provider.doesUsernameExist("helloworld"))
        assertFalse(provider.isUsernameAvailable("helloworld"))
    }

    @Test
    fun `test login but account don't exist return failure result`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<PlayerAccount>(TestMongoCollectionName.playerAccount)
        collection.drop()
        mongoDb.createCollection(TestMongoCollectionName.playerAccount)

        val db = MongoDataStore(mongoDb, TestMongoCollectionName)
        val manager = SessionManager()
        val repo = PlayerAccountRepositoryMongo(collection)
        val pcs = PlayerCreationSubunit(db)
        val provider = DefaultAuthProvider(pcs, repo, manager)

        val session = provider.login("asdf", "fdsa")
        assertTrue(session.isFailure)
    }

    @Test
    fun `test login wrong credentials return null`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<PlayerAccount>(TestMongoCollectionName.playerAccount)
        collection.drop()
        mongoDb.createCollection(TestMongoCollectionName.playerAccount)

        val db = MongoDataStore(mongoDb, TestMongoCollectionName)
        val manager = SessionManager()
        val repo = PlayerAccountRepositoryMongo(collection)
        val pcs = PlayerCreationSubunit(db)
        val provider = DefaultAuthProvider(pcs, repo, manager)

        provider.register("helloworld", "kotlinktor")
        val session = provider.login("helloworld", "ktor")
        assertTrue(session.isFailure)
    }

    @Test
    fun `test login user registered and correct credentials return non-null`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<PlayerAccount>(TestMongoCollectionName.playerAccount)
        collection.drop()
        mongoDb.createCollection(TestMongoCollectionName.playerAccount)

        val db = MongoDataStore(mongoDb, TestMongoCollectionName)
        val manager = SessionManager()
        val repo = PlayerAccountRepositoryMongo(collection)
        val pcs = PlayerCreationSubunit(db)
        val provider = DefaultAuthProvider(pcs, repo, manager)

        provider.register("helloworld", "kotlinktor")
        assertNotNull(provider.login("helloworld", "kotlinktor"))
    }
}
