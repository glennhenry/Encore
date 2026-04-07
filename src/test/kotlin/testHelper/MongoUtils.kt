package testHelper

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.Document

const val CHANGE_ME_TEST_DB_NAME = "CHANGE_ME-test-DB"
const val TEST_COLLECTION_PLAYER_ACCOUNT = "test_player_account"
const val TEST_COLLECTION_PLAYER_OBJECTS = "test_player_objects"
const val TEST_COLLECTION_SERVER_OBJECTS = "test_server_objects"
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
