package encoreTest.user

import com.mongodb.assertions.Assertions.assertFalse
import encore.account.AccountRepository
import encore.account.AccountSubunit
import encore.account.MongoAccountRepository
import encore.account.PlayerCreationSubunit
import encore.account.model.Credentials
import encore.account.model.Profile
import encore.account.model.ServerMetadata
import encore.auth.AuthSubunit
import encore.auth.LoginResult
import encore.datastore.MongoDataStore
import encore.datastore.collection.PlayerAccount
import encore.datastore.collection.PlayerId
import encore.session.SessionSubunit
import encore.utils.Outcome
import encore.utils.okOrNull
import encore.utils.okOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import testHelper.TestMongoCollectionName
import testHelper.createProfile
import testHelper.initMongo
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Integration test for [AuthSubunit] and [AccountRepository].
 *
 * Ensure MongoDB is running.
 *
 * [AuthSubunit.isUsernameAvailable] has extra logic than [AccountRepository.usernameExists]
 * try calling `repo.usernameExists("name").getOrThrow()` in addition to `isUsernameAvailable`.
 */
class TestAuthSubunit {
    private fun scope(): CoroutineScope {
        return TestScope(StandardTestDispatcher())
    }

    @Test
    fun `username shouldn't be available after user is registered`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<PlayerAccount>(TestMongoCollectionName.playerAccount)
        collection.drop()
        mongoDb.createCollection(TestMongoCollectionName.playerAccount)

        val db = MongoDataStore(mongoDb, TestMongoCollectionName)
        val manager = SessionSubunit(scope())
        val repo = MongoAccountRepository(collection)
        val accountSubunit = AccountSubunit(repo)
        val pcs = PlayerCreationSubunit(db)
        val auth = AuthSubunit(accountSubunit, pcs, manager)

        val account = PlayerAccount(
            playerId = "pid12345",
            username = "name",
            email = "anyemail",
            hashedPassword = "anypassword",
            profile = createProfile("pid12345"),
            metadata = ServerMetadata()
        )
        collection.insertOne(account)

        assertFalse(auth.isUsernameAvailable("name").okOrThrow())
        assertTrue(repo.usernameExists("name").getOrThrow())
    }

    @Test
    fun `username should be available if user is not registered`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<PlayerAccount>(TestMongoCollectionName.playerAccount)
        collection.drop()
        mongoDb.createCollection(TestMongoCollectionName.playerAccount)

        val db = MongoDataStore(mongoDb, TestMongoCollectionName)
        val manager = SessionSubunit(scope())
        val repo = MongoAccountRepository(collection)
        val accountSubunit = AccountSubunit(repo)
        val pcs = PlayerCreationSubunit(db)
        val auth = AuthSubunit(accountSubunit, pcs, manager)

        assertTrue(auth.isUsernameAvailable("xyz").okOrThrow())
        assertFalse(repo.usernameExists("xyz").getOrThrow())
    }

    @Test
    fun `register should create user`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<PlayerAccount>(TestMongoCollectionName.playerAccount)
        collection.drop()
        mongoDb.createCollection(TestMongoCollectionName.playerAccount)

        val db = MongoDataStore(mongoDb, TestMongoCollectionName)
        val manager = SessionSubunit(scope())
        val repo = MongoAccountRepository(collection)
        val accountSubunit = AccountSubunit(repo)
        val pcs = PlayerCreationSubunit(db)
        val auth = AuthSubunit(accountSubunit, pcs, manager)

        auth.register("helloworld", "kotlinktor")
        assertFalse(auth.isUsernameAvailable("helloworld").okOrThrow())
        assertTrue(repo.usernameExists("helloworld").getOrThrow())
    }

    @Test
    fun `login failures when account don't exist`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<PlayerAccount>(TestMongoCollectionName.playerAccount)
        collection.drop()
        mongoDb.createCollection(TestMongoCollectionName.playerAccount)

        val db = MongoDataStore(mongoDb, TestMongoCollectionName)
        val manager = SessionSubunit(scope())
        val repo = MongoAccountRepository(collection)
        val accountSubunit = AccountSubunit(repo)
        val pcs = PlayerCreationSubunit(db)
        val auth = AuthSubunit(accountSubunit, pcs, manager)

        val session = auth.login("asdf", "fdsa")
        // Ok = no internal error
        assertTrue((session as Outcome.Ok).value is LoginResult.AccountNotFound)
    }

    @Test
    fun `login failures when credentials are wrong`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<PlayerAccount>(TestMongoCollectionName.playerAccount)
        collection.drop()
        mongoDb.createCollection(TestMongoCollectionName.playerAccount)

        val db = MongoDataStore(mongoDb, TestMongoCollectionName)
        val manager = SessionSubunit(scope())
        val repo = MongoAccountRepository(collection)
        val accountSubunit = AccountSubunit(repo)
        val pcs = PlayerCreationSubunit(db)
        val auth = AuthSubunit(accountSubunit, pcs, manager)

        auth.register("helloworld", "kotlinktor")
        val session = auth.login("helloworld", "ktor")
        assertTrue((session as Outcome.Ok).value is LoginResult.InvalidCredentials)
    }

    @Test
    fun `login failures when repository has internal error`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<PlayerAccount>(TestMongoCollectionName.playerAccount)
        collection.drop()
        mongoDb.createCollection(TestMongoCollectionName.playerAccount)

        val db = MongoDataStore(mongoDb, TestMongoCollectionName)
        val manager = SessionSubunit(scope())
        val repo = object : AccountRepository {
            override val name: String = "AccountRepository-anonymous"
            override suspend fun getAccountByPlayerId(playerId: PlayerId): Result<PlayerAccount?> = TODO()
            override suspend fun getAccountByUsername(username: String): Result<PlayerAccount?> = TODO()
            override suspend fun getPlayerIdByUsername(username: String): Result<PlayerId?> = TODO()
            override suspend fun getCredentials(username: String): Result<Credentials?> = Result.failure(RuntimeException("xiaoting"))
            override suspend fun updatePlayerAccount(playerId: PlayerId, account: PlayerAccount): Result<Unit> = TODO()
            override suspend fun updateProfile(playerId: PlayerId, profile: Profile): Result<Unit> = TODO()
            override suspend fun updateLastActivity(playerId: PlayerId, lastActivity: Long): Result<Unit> = TODO()
            override suspend fun usernameExists(username: String): Result<Boolean> = TODO()
            override suspend fun emailExists(email: String): Result<Boolean> = TODO()
        }
        val accountSubunit = AccountSubunit(repo)
        val pcs = PlayerCreationSubunit(db)
        val auth = AuthSubunit(accountSubunit, pcs, manager)

        auth.register("helloworld", "kotlinktor")
        val session = auth.login("helloworld", "ktor")
        // should error on first call to repository in getCredentials
        assertTrue(session is Outcome.Fail)
    }

    @Test
    fun `login success when user is registered and credentials are correct`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<PlayerAccount>(TestMongoCollectionName.playerAccount)
        collection.drop()
        mongoDb.createCollection(TestMongoCollectionName.playerAccount)

        val db = MongoDataStore(mongoDb, TestMongoCollectionName)
        val manager = SessionSubunit(scope())
        val repo = MongoAccountRepository(collection)
        val accountSubunit = AccountSubunit(repo)
        val pcs = PlayerCreationSubunit(db)
        val auth = AuthSubunit(accountSubunit, pcs, manager)

        auth.register("helloworld", "kotlinktor")
        val session = auth.login("helloworld", "kotlinktor")
        assertTrue(((session as Outcome.Ok).value is LoginResult.Success))
    }
}
