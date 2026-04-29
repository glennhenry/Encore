package encoreTest.acts

import com.mongodb.assertions.Assertions.assertTrue
import encore.acts.photocard.model.ActProgress
import encore.acts.photocard.MongoPhotocardRepository
import encore.acts.photocard.model.Photocard
import encore.acts.photocard.model.SavedAct
import encore.datastore.collection.ServerObjects
import encore.utils.Ids
import kotlinx.coroutines.test.runTest
import testHelper.TestMongoCollectionName
import testHelper.initMongo
import testHelper.randomString
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MongoPhotocardRepositoryTest {
    @Test
    fun `test all`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<ServerObjects>(TestMongoCollectionName.serverObjects)
        collection.drop()
        mongoDb.createCollection(TestMongoCollectionName.serverObjects)

        val repo = MongoPhotocardRepository(collection)

        collection.insertOne(
            ServerObjects(
                acts = listOf(
                    SavedAct("player1", listOf(photocard("id1"), photocard("id2"))),
                    SavedAct("playerX", listOf(photocard(), photocard(), photocard())),
                    SavedAct("playerY", listOf(photocard(), photocard(), photocard())),
                    SavedAct("playerZ", listOf(photocard(), photocard(), photocard()))
                ),
                serverActs = listOf(photocard("serverId1")),
            )
        )

        // test getAllPhotocards
        val photocards1 = repo.getAllPhotocards("player1").getOrThrow()
        assertNotNull(photocards1.find { it.actId == "id1" })
        assertNotNull(photocards1.find { it.actId == "id2" })

        // test savePhotocard
        assertTrue(repo.savePhotocard("player1", photocard("id3")).isSuccess)
        val photocards2 = repo.getAllPhotocards("player1").getOrThrow()
        assertNotNull(photocards2.find { it.actId == "id3" })

        // test deletePhotocard
        assertTrue(repo.deletePhotocard("player1", "id2").isSuccess)
        val photocards3 = repo.getAllPhotocards("player1").getOrThrow()
        assertNull(photocards3.find { it.actId == "id2" })
        assertFalse(repo.deletePhotocard("player1", "id2").isSuccess)

        // test getServerPhotocards
        val serverPhotocards = repo.getServerPhotocards().getOrThrow()
        assertNotNull(serverPhotocards.find { it.actId == "serverId1" })

        // test saveServerPhotocard
        assertTrue(repo.saveServerPhotocard(photocard("serverId2")).isSuccess)
        val serverPhotocards2 = repo.getServerPhotocards().getOrThrow()
        assertNotNull(serverPhotocards2.find { it.actId == "serverId2" })

        // test deleteServerPhotocard
        assertTrue(repo.deleteServerPhotocard("serverId1").isSuccess)
        val serverPhotocards3 = repo.getServerPhotocards().getOrThrow()
        assertNull(photocards3.find { it.actId == "serverId1" })
        assertFalse(repo.deleteServerPhotocard("serverId1").isSuccess)
    }

    private fun photocard(id: String? = null): Photocard {
        return Photocard(id ?: Ids.uuid(), randstr(), ActProgress(0, 0, 0, 0), emptyMap())
    }

    private val charpool = ('a'..'z').toList()
    private fun randstr(): String {
        return randomString(8, charpool)
    }
}
