package testHelper

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import encore.datastore.MongoCollectionName
import org.bson.Document

const val CHANGE_ME_TEST_DB_NAME = "CHANGE_ME-test-DB"
val TestMongoCollectionName = MongoCollectionName(
    playerAccount = "test_player_account",
    playerObjects = "test_player_objects",
    serverObjects = "test_server_objects"
)
const val MONGO_TEST_URL = "mongodb://localhost:27017"

suspend fun initMongo(
    dbUrl: String = MONGO_TEST_URL,
    dbName: String = CHANGE_ME_TEST_DB_NAME
): MongoDatabase {
    val mongoc = MongoClient.create(dbUrl)
    val db = mongoc.getDatabase(dbName)
    db.runCommand(Document("ping", 1))
    return db
}
