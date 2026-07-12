package org.tasks.caldav.metadata

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.DatabaseTest
import org.tasks.data.NO_ORDER
import org.tasks.data.dao.TagFieldUpdate
import org.tasks.data.dao.METADATA_CATEGORY_TAG
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task

class TagMetadataApplyTest : DatabaseTest() {
    private val tagDataDao = db.tagDataDao()
    private val tagDao = db.tagDao()
    private val taskDao = db.taskDao()

    private suspend fun reaped(normalized: String) =
        tagDataDao.getTagsWithState().single { it.tag.normalizedName == normalized }.reaped

    private suspend fun dirty(normalized: String) =
        tagDataDao.getTagsWithState().single { it.tag.normalizedName == normalized }.dirty

    private suspend fun dirtyVersion(normalized: String) =
        tagDataDao.getTagsWithState().single { it.tag.normalizedName == normalized }.dirtyVersion

    @Test
    fun createsUpdatesMarksReapedAndOrders() = runBlocking {
        val workId = tagDataDao.insert(TagData(name = "Work"))
        val oldId = tagDataDao.insert(TagData(name = "Old", color = -1))

        tagDataDao.applyMetadataMerge(
            create = listOf(TagData(name = "Home", color = -5, icon = "house")),
            update = listOf(TagFieldUpdate(workId, remoteId = null, name = "Work", color = -12345, icon = "briefcase")),
            markReaped = listOf(oldId),
            clearReaped = emptyList(),
            orderWrites = listOf("home" to 0, "work" to 1),
        )

        val byKey = tagDataDao.getAll().associateBy { it.normalizedName }

        assertEquals(true, reaped("old"))
        tagDataDao.reapOrphanedTombstones()
        assertNull(tagDataDao.getAll().firstOrNull { it.normalizedName == "old" })
        val home = byKey.getValue("home")
        assertEquals("Home", home.name)
        assertEquals(-5, home.color)
        assertEquals("house", home.icon)
        val work = byKey.getValue("work")
        assertEquals("Work", work.name)
        assertEquals(-12345, work.color)
        assertEquals("briefcase", work.icon)
        assertEquals(0, home.order)
        assertEquals(1, work.order)
    }

    @Test
    fun reapSweepKeepsTombstonedTagStillInUse() = runBlocking {
        val tag = TagData(name = "Work")
        val id = tagDataDao.insert(tag)
        val task = Task(title = "a task")
        taskDao.createNew(task)
        tagDao.insert(Tag(task = task.id, tagUid = tag.remoteId!!, name = "Work", taskUid = task.uuid))

        tagDataDao.applyMetadataMerge(
            create = emptyList(),
            update = emptyList(),
            markReaped = listOf(id),
            clearReaped = emptyList(),
            orderWrites = emptyList(),
        )
        tagDataDao.reapOrphanedTombstones()

        assertNotNull(tagDataDao.getAll().firstOrNull { it.normalizedName == "work" })
    }

    @Test
    fun createIgnoresNormalizedNameCollision() = runBlocking {
        tagDataDao.insert(TagData(name = "Work", color = -1))

        tagDataDao.applyMetadataMerge(
            create = listOf(TagData(name = "WORK", color = -999)),
            update = emptyList(),
            markReaped = emptyList(),
            clearReaped = emptyList(),
            orderWrites = emptyList(),
        )

        val all = tagDataDao.getAll()
        assertEquals(1, all.size)
        assertEquals("Work", all.single().name)
        assertEquals(-1, all.single().color)
    }

    @Test
    fun chunksReapedListsLargerThanSqliteVariableLimit() = runBlocking {
        val ids = (1..1001).map { tagDataDao.insert(TagData(name = "tag$it")) }

        tagDataDao.applyMetadataMerge(
            create = emptyList(),
            update = emptyList(),
            markReaped = ids,
            clearReaped = emptyList(),
            orderWrites = emptyList(),
        )
        assertEquals(1001, tagDataDao.getTagsWithState().count { it.reaped })

        tagDataDao.applyMetadataMerge(
            create = emptyList(),
            update = emptyList(),
            markReaped = emptyList(),
            clearReaped = ids,
            orderWrites = emptyList(),
        )
        assertEquals(0, tagDataDao.getTagsWithState().count { it.reaped })
    }

    @Test
    fun unorderedTagsKeepNoOrder() = runBlocking {
        tagDataDao.insert(TagData(name = "Solo"))

        tagDataDao.applyMetadataMerge(
            create = emptyList(),
            update = emptyList(),
            markReaped = emptyList(),
            clearReaped = emptyList(),
            orderWrites = emptyList(),
        )

        assertEquals(NO_ORDER, tagDataDao.getAll().single().order)
    }

    @Test
    fun casingChangePropagatesToTagsJoinRows() = runBlocking {
        val tag = TagData(name = "work")
        val uid = tag.remoteId!!
        val id = tagDataDao.insert(tag)
        val task = Task(title = "a task")
        taskDao.createNew(task)
        tagDao.insert(Tag(task = task.id, tagUid = uid, name = "work", taskUid = task.uuid))

        tagDataDao.applyMetadataMerge(
            create = emptyList(),
            update = listOf(TagFieldUpdate(id, uid, name = "WORK", color = 0, icon = null)),
            markReaped = emptyList(),
            clearReaped = emptyList(),
            orderWrites = emptyList(),
        )

        assertEquals("WORK", tagDataDao.getAll().single().name)
        assertEquals("WORK", tagDao.getByTagUid(uid).single().name)
    }

    @Test
    fun markSyncedClearsDirtyOnlyUpToThePushedVersion() = runBlocking {
        val id = tagDataDao.createDirty(TagData(name = "Work", color = -5))!!.id!!
        val pushedVersion = dirtyVersion("work")

        db.metadataDao().markDirty(METADATA_CATEGORY_TAG, listOf(id))
        tagDataDao.markSynced(id, pushedVersion)
        assertEquals(true, dirty("work"))

        tagDataDao.markSynced(id, dirtyVersion("work"))
        assertEquals(false, dirty("work"))
    }

    @Test
    fun editTagDoesNotRevertAnUntouchedFieldChangedConcurrently() = runBlocking {
        tagDataDao.insert(TagData(name = "Work", color = -5))
        val remoteId = tagDataDao.getAll().single().remoteId!!
        tagDataDao.update(tagDataDao.getAll().single().copy(color = -9))

        val ok = tagDataDao.editTag(
            remoteId, name = "Work", color = -5, icon = "star",
            nameChanged = false, colorChanged = false, iconChanged = true,
        )

        assertTrue(ok)
        val updated = tagDataDao.getAll().single()
        assertEquals("star", updated.icon)
        assertEquals(-9, updated.color)
        assertEquals(true, dirty("work"))
    }

    @Test
    fun editTagRematerializesARowSweptConcurrently() = runBlocking {
        tagDataDao.insert(TagData(name = "Work", color = -5))
        val remoteId = tagDataDao.getAll().single().remoteId!!
        tagDataDao.delete(tagDataDao.getAll().single())
        assertNull(tagDataDao.getAll().firstOrNull())

        val ok = tagDataDao.editTag(
            remoteId, name = "Work", color = -7, icon = "star",
            nameChanged = false, colorChanged = true, iconChanged = true,
        )

        assertTrue(ok)
        val restored = tagDataDao.getAll().single()
        assertEquals("Work", restored.name)
        assertEquals(-7, restored.color)
        assertEquals("star", restored.icon)
        assertEquals(true, dirty("work"))
        assertEquals(false, reaped("work"))
    }

    @Test
    fun rescueReapedResolvesAKeyRecreatedWithANewRemoteId() = runBlocking {
        tagDataDao.insert(TagData(name = "Work", color = -5))
        val stalePick = tagDataDao.getAll().single()
        tagDataDao.delete(stalePick)
        tagDataDao.insert(TagData(name = "Work"))
        val canonicalRemoteId = tagDataDao.getAll().single().remoteId

        val resolved = tagDataDao.rescueReaped(listOf(stalePick))

        assertEquals(1, resolved.size)
        assertEquals(canonicalRemoteId, resolved.single().remoteId)
        assertEquals(1, tagDataDao.getAll().size)
    }

    @Test
    fun deletingATagCleansUpItsSyncStateViaTheTrigger() = runBlocking {
        tagDataDao.createDirty(TagData(name = "Work"))
        assertTrue(tagDataDao.hasDirty())

        tagDataDao.delete(tagDataDao.getAll().single())

        assertEquals(false, tagDataDao.hasDirty())
    }

    @Test
    fun deleteWithTombstoneQueuesOutboundDeleteAndRemovesRow() = runBlocking {
        val tag = TagData(name = "Work")
        tagDataDao.insert(tag)

        tagDataDao.deleteWithTombstone(tagDataDao.getAll().single())

        assertNull(tagDataDao.getAll().firstOrNull())
        assertEquals(listOf("work"), tagDataDao.getTombstoneKeys())
    }

    @Test
    fun renameTagQueuesOldKeyTombstoneAndDirtiesNewRow() = runBlocking {
        tagDataDao.insert(TagData(name = "Work", color = -5))
        val remoteId = tagDataDao.getAll().single().remoteId!!

        val row = tagDataDao.renameTag(remoteId, "Job", color = -5, icon = null, colorChanged = false, iconChanged = false)

        assertNotNull(row)
        assertEquals("Job", tagDataDao.getAll().single().name)
        assertEquals("job", tagDataDao.getAll().single().normalizedName)
        assertEquals(listOf("work"), tagDataDao.getTombstoneKeys())
        assertEquals(true, dirty("job"))
    }

    @Test
    fun renameTagCaseOnlyKeepsTheKeyAndQueuesNoTombstone() = runBlocking {
        tagDataDao.insert(TagData(name = "work"))
        val remoteId = tagDataDao.getAll().single().remoteId!!

        val row = tagDataDao.renameTag(remoteId, "WORK", color = 0, icon = null, colorChanged = false, iconChanged = false)

        assertNotNull(row)
        assertEquals("WORK", tagDataDao.getAll().single().name)
        assertTrue(tagDataDao.getTombstoneKeys().isEmpty())
        assertEquals(true, dirty("work"))
    }

    @Test
    fun createDirtyAtAJustDeletedKeyCancelsThePendingDelete() = runBlocking {
        tagDataDao.insert(TagData(name = "Work"))
        tagDataDao.deleteWithTombstone(tagDataDao.getAll().single())
        assertEquals(listOf("work"), tagDataDao.getTombstoneKeys())

        tagDataDao.createDirty(TagData(name = "Work"))

        assertTrue(tagDataDao.getTombstoneKeys().isEmpty())
        assertEquals(true, dirty("work"))
    }

    @Test
    fun renameIntoAJustDeletedKeyCancelsThatKeysPendingDelete() = runBlocking {
        tagDataDao.insert(TagData(name = "Work"))
        tagDataDao.insert(TagData(name = "Job"))
        tagDataDao.deleteWithTombstone(tagDataDao.getAll().single { it.normalizedName == "job" })
        assertEquals(listOf("job"), tagDataDao.getTombstoneKeys())
        val workId = tagDataDao.getAll().single { it.normalizedName == "work" }.remoteId!!

        tagDataDao.renameTag(workId, "Job", color = 0, icon = null, colorChanged = false, iconChanged = false)

        assertEquals(listOf("work"), tagDataDao.getTombstoneKeys())
        assertEquals(true, dirty("job"))
    }

    @Test
    fun renameTagReturnsNullOnAClash() = runBlocking {
        tagDataDao.insert(TagData(name = "Work"))
        tagDataDao.insert(TagData(name = "Home"))
        val workId = tagDataDao.getAll().single { it.normalizedName == "work" }.remoteId!!

        val row = tagDataDao.renameTag(workId, "Home", color = 0, icon = null, colorChanged = false, iconChanged = false)

        assertNull(row)
        assertTrue(tagDataDao.getTombstoneKeys().isEmpty())
        assertEquals("Work", tagDataDao.getAll().single { it.normalizedName == "work" }.name)
    }
}
