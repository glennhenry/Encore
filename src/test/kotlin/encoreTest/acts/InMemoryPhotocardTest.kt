package encoreTest.acts

import com.mongodb.assertions.Assertions.assertTrue
import encore.acts.photocard.InMemoryPhotocardRepository
import encore.acts.photocard.model.ActProgress
import encore.acts.photocard.model.Photocard
import encore.datastore.collection.ServerId
import encore.utils.Ids
import kotlinx.coroutines.test.runTest
import testHelper.randomString
import kotlin.collections.set
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InMemoryPhotocardTest {
    @Test
    fun `test all`() = runTest {
        val repo = InMemoryPhotocardRepository {
            set("player1", mutableListOf(photocard("id1"), photocard("id2")))
            set("playerX", mutableListOf(photocard(), photocard(), photocard()))
            set("playerY", mutableListOf(photocard(), photocard(), photocard()))
            set("playerZ", mutableListOf(photocard(), photocard(), photocard()))
            set(ServerId, mutableListOf(photocard("serverId1")))
        }

        // test getAllPhotocards
        val photocards1 = repo.getAllPhotocards("player1").getOrThrow()
        assertNotNull(photocards1.find { it.actId == "id1" })
        assertNotNull(photocards1.find { it.actId == "id2" })

        // test savePhotocard
        val pc3 = photocard("id3")
        assertTrue(repo.savePhotocard("player1", pc3).isSuccess)
        val photocards2 = repo.getAllPhotocards("player1").getOrThrow()
        assertNotNull(photocards2.find { it.actId == "id3" })

        // test deletePhotocard
        assertTrue(repo.deletePhotocard("player1", "id2").isSuccess)
        val photocards3 = repo.getAllPhotocards("player1").getOrThrow()
        assertNull(photocards3.find { it.actId == "id2" })
        assertFalse(repo.deletePhotocard("player1", "id2").isSuccess)

        // test updatePhotocard
        assertTrue(repo.updatePhotocard("player1", pc3.copy(name = "ABCDEFGirl")).isSuccess)
        val photocards4 = repo.getAllPhotocards("player1").getOrThrow()
        assertEquals("ABCDEFGirl", photocards4.find { it.actId == "id3" }?.name)

        // test getServerPhotocards
        val serverPhotocards = repo.getServerPhotocards().getOrThrow()
        assertNotNull(serverPhotocards.find { it.actId == "serverId1" })

        // test saveServerPhotocard
        val spc2 = photocard("serverId2")
        assertTrue(repo.saveServerPhotocard(spc2).isSuccess)
        val serverPhotocards2 = repo.getServerPhotocards().getOrThrow()
        assertNotNull(serverPhotocards2.find { it.actId == "serverId2" })

        // test deleteServerPhotocard
        assertTrue(repo.deleteServerPhotocard("serverId1").isSuccess)
        val serverPhotocards3 = repo.getServerPhotocards().getOrThrow()
        assertNull(serverPhotocards3.find { it.actId == "serverId1" })
        assertFalse(repo.deleteServerPhotocard("serverId1").isSuccess)

        // test updateServerPhotocard
        assertTrue(repo.updateServerPhotocard(spc2.copy(name = "ABCDEFGirl")).isSuccess)
        val serverPhotocards4 = repo.getServerPhotocards().getOrThrow()
        assertEquals("ABCDEFGirl", serverPhotocards4.find { it.actId == "serverId2" }?.name)
    }

    private fun photocard(id: String? = null): Photocard {
        return Photocard(id ?: Ids.uuid(), randstr(), ActProgress(0, 0, 0), emptyMap())
    }

    private val charpool = ('a'..'z').toList()
    private fun randstr(): String {
        return randomString(8, charpool)
    }

}
