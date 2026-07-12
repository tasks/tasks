package org.tasks.caldav.metadata

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.tasks.DatabaseTest
import org.tasks.caldav.CaldavClient
import org.tasks.caldav.CaldavClientProvider
import org.tasks.caldav.VtodoCache
import org.tasks.data.NO_ORDER
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.preferences.TasksPreferences

class TagMetadataSyncTest : DatabaseTest() {
    private val caldavDao = db.caldavDao()
    private val tagDataDao = db.tagDataDao()
    private val tagDao = db.tagDao()
    private val taskDao = db.taskDao()
    private val vtodoCache = mock<VtodoCache>()
    private val preferences = TasksPreferences(InMemoryDataStore())
    private val sync = TagMetadataSync(caldavDao, tagDataDao, mock<CaldavClientProvider>(), vtodoCache, preferences)
    private val principal = "https://example.com/principal/".toHttpUrl()

    private class InMemoryDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private suspend fun insertPrimaryAccount(): CaldavAccount {
        val account = caldavDao.getAccount(caldavDao.insert(CaldavAccount()))!!
        preferences.set(TasksPreferences.metadataPrimaryAccount, account.id!!)
        preferences.set(TasksPreferences.metadataStoreAccount, account.id!!)
        return account
    }

    private suspend fun rev(): String = preferences.get(TasksPreferences.metadataRev, "")

    private suspend fun reaped(normalized: String) =
        tagDataDao.getTagsWithState().single { it.tag.normalizedName == normalized }.reaped

    private suspend fun dirty(normalized: String) =
        tagDataDao.getTagsWithState().single { it.tag.normalizedName == normalized }.dirty

    private suspend fun insertDirty(tag: TagData): TagData = tagDataDao.createDirty(tag)!!

    private suspend fun insertReaped(tag: TagData): TagData {
        tagDataDao.insert(tag)
        sync.applyRemote("""{"version":1,"tags":{"${tag.normalizedName}":{"deleted":true,"ts":1}}}""")
        return tagDataDao.getByUuid(tag.remoteId!!)!!
    }

    @Test
    fun `absent blob is a no-op`() = runBlocking {
        tagDataDao.insert(TagData(name = "Work", color = -5))

        sync.applyRemote(null)
        sync.applyRemote("")
        sync.applyRemote("   ")

        assertEquals(listOf("Work"), tagDataDao.getAll().map { it.name })
        assertEquals(-5, tagDataDao.getAll().single().color)
    }

    @Test
    fun `a deeply nested array payload is a no-op, not a crash`() = runBlocking {
        tagDataDao.insert(TagData(name = "Work", color = -5))
        val bomb = "[".repeat(100000) + "]".repeat(100000)

        sync.applyRemote(bomb)

        assertEquals(-5, tagDataDao.getAll().single().color)
    }

    @Test
    fun `live entry with no local tag is created`() = runBlocking {
        sync.applyRemote("""{"version":1,"tags":{"work":{"name":"Work","color":-5,"icon":"star"}},"order":[]}""")

        val tag = tagDataDao.getAll().single()
        assertEquals("Work", tag.name)
        assertEquals(-5, tag.color)
        assertEquals("star", tag.icon)
        assertEquals("work", tag.normalizedName)
    }

    @Test
    fun `live entry updates decoration and propagates a casing change`() = runBlocking {
        tagDataDao.insert(TagData(name = "Work", color = 0))

        sync.applyRemote("""{"version":1,"tags":{"work":{"name":"WORK","color":-9,"icon":"star"}},"order":[]}""")

        val tag = tagDataDao.getAll().single()
        assertEquals(-9, tag.color)
        assertEquals("star", tag.icon)
        assertEquals("WORK", tag.name)
        assertEquals("work", tag.normalizedName)
    }

    @Test
    fun `create keeps the store's name only when it folds to the key`() = runBlocking {
        sync.applyRemote("""{"version":1,"tags":{"work":{"name":"Not Work"}}}""")

        val tag = tagDataDao.getAll().single()
        assertEquals("work", tag.name)
        assertEquals("work", tag.normalizedName)
    }

    @Test
    fun `tombstone marks a tag reaped, and the sweep deletes it when orphaned`() = runBlocking {
        tagDataDao.insert(TagData(name = "Work"))

        sync.applyRemote("""{"version":1,"tags":{"work":{"deleted":true,"ts":1000}},"order":[]}""")
        assertEquals(true, reaped("work"))

        tagDataDao.reapOrphanedTombstones()
        assertNull(tagDataDao.getAll().firstOrNull { it.name == "Work" })
    }

    @Test
    fun `the sweep keeps a tombstoned tag that still has tasks`() = runBlocking {
        val tag = TagData(name = "Work")
        tagDataDao.insert(tag)
        val task = Task(title = "a task")
        taskDao.createNew(task)
        tagDao.insert(Tag(task = task.id, tagUid = tag.remoteId!!, name = "Work", taskUid = task.uuid))

        sync.applyRemote("""{"version":1,"tags":{"work":{"deleted":true,"ts":1000}},"order":[]}""")
        tagDataDao.reapOrphanedTombstones()

        assertEquals(listOf("Work"), tagDataDao.getAll().map { it.name })
        assertEquals(true, reaped("work"))
    }

    @Test
    fun `rename tombstones the old name and creates the new, sweep removes the orphaned old`() = runBlocking {
        tagDataDao.insert(TagData(name = "Work"))

        sync.applyRemote("""{"version":1,"tags":{"work":{"deleted":true,"ts":1000},"job":{"name":"Job"}},"order":[]}""")
        tagDataDao.reapOrphanedTombstones()

        assertEquals(listOf("Job"), tagDataDao.getAll().map { it.name })
    }

    @Test
    fun `a live entry un-reaps a recreated tag`() = runBlocking {
        tagDataDao.insert(TagData(name = "Work"))
        sync.applyRemote("""{"version":1,"tags":{"work":{"deleted":true,"ts":1}},"order":[]}""")
        assertEquals(true, reaped("work"))

        sync.applyRemote("""{"version":1,"tags":{"work":{"name":"Work"}},"order":[]}""")
        assertEquals(false, reaped("work"))

        tagDataDao.reapOrphanedTombstones()
        assertNotNull(tagDataDao.getAll().firstOrNull { it.name == "Work" })
    }

    @Test
    fun `apply resets a locally-ordered tag absent from the store's order`() = runBlocking {
        val aId = tagDataDao.insert(TagData(name = "A"))
        val bId = tagDataDao.insert(TagData(name = "B"))
        tagDataDao.setOrder(aId, 0)
        tagDataDao.setOrder(bId, 1)

        sync.applyRemote("""{"version":1,"tags":{"a":{"name":"A"},"b":{"name":"B"}},"order":["a"]}""")

        assertEquals(0, tagDataDao.getAll().single { it.normalizedName == "a" }.order)
        assertEquals(NO_ORDER, tagDataDao.getAll().single { it.normalizedName == "b" }.order)
    }

    @Test
    fun `apply with an empty order resets every local tag's order`() = runBlocking {
        val aId = tagDataDao.insert(TagData(name = "A"))
        tagDataDao.setOrder(aId, 0)

        sync.applyRemote("""{"version":1,"tags":{"a":{"name":"A"}},"order":[]}""")

        assertEquals(NO_ORDER, tagDataDao.getAll().single().order)
    }

    @Test
    fun `apply leaves local order untouched when the blob omits order entirely`() = runBlocking {
        val aId = tagDataDao.insert(TagData(name = "A"))
        tagDataDao.setOrder(aId, 3)

        sync.applyRemote("""{"version":1,"tags":{"a":{"name":"A"}}}""")

        assertEquals(3, tagDataDao.getAll().single().order)
    }

    @Test
    fun `pull skips the payload read when the stored rev matches the remote version`() = runBlocking {
        val account = insertPrimaryAccount()
        preferences.set(TasksPreferences.metadataRev, "v1")
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn "v1"
        }

        sync.pull(account, client, principal)

        verify(client, never()).tagMetadata(any())
        Unit
    }

    @Test
    fun `applying records the rev pointer`() = runBlocking {
        val account = insertPrimaryAccount()
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn "v2"
            onBlocking { tagMetadata(any()) } doReturn
                    """{"rev":"v2","version":1,"tags":{"work":{"name":"Work"}},"order":[]}"""
        }

        sync.pull(account, client, principal)
        assertEquals("v2", rev())
    }

    @Test
    fun `pull applies the payload and caches it when the version changed`() = runBlocking {
        val base = """{"rev":"v1","version":1,"tags":{"work":{"name":"Work"}},"order":[]}"""
        tagDataDao.insert(TagData(name = "Work"))
        val cached = mock<VtodoCache> { onBlocking { getTagMetadata(any()) } doReturn base }
        val sync = TagMetadataSync(caldavDao, tagDataDao, mock<CaldavClientProvider>(), cached, preferences)
        val account = insertPrimaryAccount()
        preferences.set(TasksPreferences.metadataRev, "v1")
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn "v2"
            onBlocking { tagMetadata(any()) } doReturn
                    """{"rev":"v2","version":1,"tags":{"work":{"deleted":true,"ts":1}},"order":[]}"""
        }

        sync.pull(account, client, principal)

        assertEquals(true, reaped("work"))
        verify(cached).putTagMetadata(any(), any())
        Unit
    }

    @Test
    fun `pushDirty does not clobber a peer's concurrent edit to an untouched field`() = runBlocking {
        val base = """{"rev":"v1","version":1,"tags":{"work":{"name":"Work","color":-5,"icon":"star"}},"order":[]}"""
        val cached = mock<VtodoCache> { onBlocking { getTagMetadata(any()) } doReturn base }
        val sync = TagMetadataSync(caldavDao, tagDataDao, mock<CaldavClientProvider>(), cached, preferences)
        insertDirty(TagData(name = "Work", color = -9, icon = "star"))
        val account = insertPrimaryAccount()
        preferences.set(TasksPreferences.metadataRev, "v1")
        var pushed: String? = null
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn "v2"
            onBlocking { tagMetadata(any()) } doReturn
                    """{"rev":"v2","version":1,"tags":{"work":{"name":"Work","color":-5,"icon":"moon"}},"order":[]}"""
            onBlocking { pushTagMetadata(any(), any(), any()) } doAnswer { pushed = it.getArgument(1); true }
        }

        sync.pushDirty(account, client, principal)

        val entry = TagMetadataBlob.parse(pushed)!!.entryOf("work") as Entry.Live
        assertEquals(-9, entry.color)
        assertEquals("moon", entry.icon)
    }

    @Test
    fun `first adoption fills local decoration the store left unset and pushes the union`() = runBlocking {
        tagDataDao.insert(TagData(name = "Work", color = -5))
        var pushed: String? = null
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn "v1"
            onBlocking { tagMetadata(any()) } doReturn """{"rev":"v1","version":1,"tags":{"work":{"name":"Work"}},"order":[]}"""
            onBlocking { pushTagMetadata(any(), any(), any()) } doAnswer { pushed = it.getArgument(1); true }
        }

        sync.pull(insertPrimaryAccount(), client, principal)

        assertEquals(-5, tagDataDao.getAll().single { it.normalizedName == "work" }.color)
        verify(client).pushTagMetadata(any(), any(), any())
        assertTrue(pushed!!.contains("-5"))
    }

    @Test
    fun `first adoption contributes a local-only tag to the store`() = runBlocking {
        tagDataDao.insert(TagData(name = "Personal", color = -7))
        var pushed: String? = null
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn "v1"
            onBlocking { tagMetadata(any()) } doReturn """{"rev":"v1","version":1,"tags":{"work":{"name":"Work"}},"order":[]}"""
            onBlocking { pushTagMetadata(any(), any(), any()) } doAnswer { pushed = it.getArgument(1); true }
        }

        sync.pull(insertPrimaryAccount(), client, principal)

        val names = tagDataDao.getAll().map { it.name }.toSet()
        assertTrue(names.contains("Personal"))
        assertTrue(names.contains("Work"))
        verify(client).pushTagMetadata(any(), any(), any())
        assertTrue(pushed!!.contains("personal"))
    }

    @Test
    fun `first adoption keeps a pending local edit that conflicts with a store-set value`() = runBlocking {
        insertDirty(TagData(name = "Work", color = -5))
        var pushed: String? = null
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn "v1"
            onBlocking { tagMetadata(any()) } doReturn """{"rev":"v1","version":1,"tags":{"work":{"name":"Work","color":-9}},"order":[]}"""
            onBlocking { pushTagMetadata(any(), any(), any()) } doAnswer { pushed = it.getArgument(1); true }
        }

        sync.pull(insertPrimaryAccount(), client, principal)

        assertEquals(-5, tagDataDao.getAll().single { it.normalizedName == "work" }.color)
        assertTrue(pushed!!.contains("-5"))
        assertFalse(pushed!!.contains("-9"))
    }

    @Test
    fun `first adoption adopts the store's decoration over a conflicting non-dirty local value`() = runBlocking {
        tagDataDao.insert(TagData(name = "Work", color = -5))
        var pushed: String? = null
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn "v1"
            onBlocking { tagMetadata(any()) } doReturn """{"rev":"v1","version":1,"tags":{"work":{"name":"Work","color":-9}},"order":[]}"""
            onBlocking { pushTagMetadata(any(), any(), any()) } doAnswer { pushed = it.getArgument(1); true }
        }

        sync.pull(insertPrimaryAccount(), client, principal)

        assertEquals(-9, tagDataDao.getAll().single { it.normalizedName == "work" }.color)
        verify(client, never()).pushTagMetadata(any(), any(), any())
        assertNull(pushed)
    }

    @Test
    fun `first adoption resurrects a dirty tag the store tombstoned`() = runBlocking {
        insertDirty(TagData(name = "Work", color = -5))
        var pushed: String? = null
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn "v1"
            onBlocking { tagMetadata(any()) } doReturn """{"rev":"v1","version":1,"tags":{"work":{"deleted":true,"ts":1}},"order":[]}"""
            onBlocking { pushTagMetadata(any(), any(), any()) } doAnswer { pushed = it.getArgument(1); true }
        }

        sync.pull(insertPrimaryAccount(), client, principal)

        assertEquals(false, reaped("work"))
        assertEquals(false, dirty("work"))
        verify(client).pushTagMetadata(any(), any(), any())
        assertFalse(pushed!!.contains("deleted"))
    }

    @Test
    fun `first adoption fills an unset field of a dirty tag from the store`() = runBlocking {
        insertDirty(TagData(name = "Work", color = 0))
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn "v1"
            onBlocking { tagMetadata(any()) } doReturn
                    """{"rev":"v1","version":1,"tags":{"work":{"name":"work","color":-5}},"order":[]}"""
            onBlocking { pushTagMetadata(any(), any(), any()) } doReturn true
        }

        sync.pull(insertPrimaryAccount(), client, principal)

        val work = tagDataDao.getAll().single { it.normalizedName == "work" }
        assertEquals("Work", work.name)
        assertEquals(-5, work.color)
    }

    @Test
    fun `pushDirty upserts dirty tags as live entries and clears the flag`() = runBlocking {
        insertDirty(TagData(name = "Home", color = -3))
        val account = insertPrimaryAccount()
        var pushed: String? = null
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn null
            onBlocking { tagMetadata(any()) } doReturn null
            onBlocking { pushTagMetadata(any(), any(), any()) } doAnswer { pushed = it.getArgument(1); true }
        }

        sync.pushDirty(account, client, principal)

        verify(client).pushTagMetadata(any(), any(), any())
        assertTrue(pushed!!.contains("home"))
        assertEquals(false, dirty("home"))
    }

    @Test
    fun `pushDirty is a no-op when nothing is pending`() = runBlocking {
        tagDataDao.insert(TagData(name = "Work"))
        val client = mock<CaldavClient>()

        sync.pushDirty(CaldavAccount(id = 1), client, principal)

        verify(client, never()).pushTagMetadata(any(), any(), any())
        Unit
    }

    @Test
    fun `an async delete pushes a tombstone and drops the queue`() = runBlocking {
        tagDataDao.insert(TagData(name = "Work"))
        val account = insertPrimaryAccount()
        var pushed: String? = null
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn null
            onBlocking { tagMetadata(any()) } doReturn """{"rev":"v1","version":1,"tags":{"work":{"name":"Work"}},"order":[]}"""
            onBlocking { pushTagMetadata(any(), any(), any()) } doAnswer { pushed = it.getArgument(1); true }
        }

        sync.deleteTag(tagDataDao.getAll().single())
        assertNull(tagDataDao.getAll().firstOrNull())
        assertEquals(listOf("work"), tagDataDao.getTombstoneKeys())

        sync.pushDirty(account, client, principal)

        assertTrue(TagMetadataBlob.parse(pushed)!!.entryOf("work") is Entry.Tomb)
        assertTrue(tagDataDao.getTombstoneKeys().isEmpty())
    }

    @Test
    fun `a pending outbound delete suppresses a concurrent live pull`() = runBlocking {
        insertPrimaryAccount()
        tagDataDao.insert(TagData(name = "Work"))
        sync.deleteTag(tagDataDao.getAll().single())
        assertEquals(listOf("work"), tagDataDao.getTombstoneKeys())

        sync.applyRemote("""{"version":1,"tags":{"work":{"name":"Work"}}}""")

        assertNull(tagDataDao.getAll().firstOrNull { it.normalizedName == "work" })
        assertEquals(listOf("work"), tagDataDao.getTombstoneKeys())
    }

    @Test
    fun `an async rename does not revert a concurrent update to an untouched field, and pushes it`() = runBlocking {
        tagDataDao.insert(TagData(name = "Work", color = -5))
        val old = tagDataDao.getAll().single()
        tagDataDao.update(tagDataDao.getAll().single().copy(color = -9))
        val account = insertPrimaryAccount()
        var pushed: String? = null
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn null
            onBlocking { tagMetadata(any()) } doReturn null
            onBlocking { pushTagMetadata(any(), any(), any()) } doAnswer { pushed = it.getArgument(1); true }
        }

        val row = sync.renameTag(old.remoteId!!, "Job", color = -5, icon = old.icon, colorChanged = false, iconChanged = false)
        assertNotNull(row)
        sync.pushDirty(account, client, principal)

        val job = tagDataDao.getAll().single()
        assertEquals("Job", job.name)
        assertEquals(-9, job.color)
        assertTrue(pushed!!.contains("-9"))
        assertFalse(pushed!!.contains("-5"))
        val blob = TagMetadataBlob.parse(pushed)!!
        assertTrue(blob.entryOf("work") is Entry.Tomb)
        assertTrue(blob.entryOf("job") is Entry.Live)
    }

    @Test
    fun `a refused push leaves an async rename queued`() = runBlocking {
        tagDataDao.insert(TagData(name = "Work"))
        val old = tagDataDao.getAll().single()
        val account = insertPrimaryAccount()
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn null
            onBlocking { tagMetadata(any()) } doReturn null
            onBlocking { pushTagMetadata(any(), any(), any()) } doReturn false
        }

        assertNotNull(sync.renameTag(old.remoteId!!, "Job", 0, null, colorChanged = false, iconChanged = false))
        sync.pushDirty(account, client, principal)

        assertEquals("Job", tagDataDao.getAll().single().name)
        assertEquals(listOf("work"), tagDataDao.getTombstoneKeys())
        assertEquals(true, dirty("job"))
    }

    @Test
    fun `applyRemote does not overwrite a locally-dirty tag`() = runBlocking {
        insertDirty(TagData(name = "Work", color = -5))

        sync.applyRemote("""{"version":1,"tags":{"work":{"name":"Work","color":-9}},"order":[]}""")

        assertEquals(-5, tagDataDao.getAll().single { it.normalizedName == "work" }.color)
    }

    @Test
    fun `applyRemote does not reap a locally-dirty tag`() = runBlocking {
        insertDirty(TagData(name = "Work"))

        sync.applyRemote("""{"version":1,"tags":{"work":{"deleted":true,"ts":1}},"order":[]}""")

        assertEquals(false, reaped("work"))
    }

    @Test
    fun `pull seeds an absent store from local tags on first activation`() = runBlocking {
        tagDataDao.insert(TagData(name = "Work", color = -5))
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn null
            onBlocking { tagMetadata(any()) } doReturn null
            onBlocking { pushTagMetadata(any(), any(), any()) } doReturn true
        }

        sync.pull(insertPrimaryAccount(), client, principal)

        verify(client).pushTagMetadata(any(), any(), any())
        Unit
    }

    @Test
    fun `pull does not re-seed an absent store when this device already cached it`() = runBlocking {
        tagDataDao.insert(TagData(name = "Work", color = -5))
        val cached = mock<VtodoCache> {
            onBlocking { getTagMetadata(any()) } doReturn """{"rev":"old","version":1,"tags":{},"order":[]}"""
        }
        val sync = TagMetadataSync(caldavDao, tagDataDao, mock<CaldavClientProvider>(), cached, preferences)
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn null
            onBlocking { tagMetadata(any()) } doReturn null
        }

        sync.pull(insertPrimaryAccount(), client, principal)

        verify(client, never()).pushTagMetadata(any(), any(), any())
        Unit
    }

    @Test
    fun `steady-state pull adopts a remote change to a field the user has not locally edited`() = runBlocking {
        val base = """{"rev":"v1","version":1,"tags":{"work":{"name":"Work","color":-5}},"order":[]}"""
        insertDirty(TagData(name = "Work", color = -9))
        val cached = mock<VtodoCache> { onBlocking { getTagMetadata(any()) } doReturn base }
        val sync = TagMetadataSync(caldavDao, tagDataDao, mock<CaldavClientProvider>(), cached, preferences)
        val account = insertPrimaryAccount()
        preferences.set(TasksPreferences.metadataRev, "v1")
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn "v2"
            onBlocking { tagMetadata(any()) } doReturn
                    """{"rev":"v2","version":1,"tags":{"work":{"name":"WORK","color":-5}},"order":[]}"""
        }

        sync.pull(account, client, principal)

        val work = tagDataDao.getAll().single { it.normalizedName == "work" }
        assertEquals("WORK", work.name)
        assertEquals(-9, work.color)
    }

    @Test
    fun `steady-state pull re-dirties a local tag the store dropped without a tombstone`() = runBlocking {
        val base = """{"rev":"v1","version":1,"tags":{"foo":{"name":"Foo"},"bar":{"name":"Bar"}},"order":[]}"""
        tagDataDao.insert(TagData(name = "Foo"))
        tagDataDao.insert(TagData(name = "Bar"))
        val cached = mock<VtodoCache> { onBlocking { getTagMetadata(any()) } doReturn base }
        val sync = TagMetadataSync(caldavDao, tagDataDao, mock<CaldavClientProvider>(), cached, preferences)
        val account = insertPrimaryAccount()
        preferences.set(TasksPreferences.metadataRev, "v1")
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn "v2"
            onBlocking { tagMetadata(any()) } doReturn """{"rev":"v2","version":1,"tags":{"bar":{"name":"Bar"}},"order":[]}"""
        }

        sync.pull(account, client, principal)

        assertEquals(true, dirty("foo"))
        assertEquals(false, dirty("bar"))
    }

    @Test
    fun `steady-state pull does not resurrect a reaped tag the store dropped`() = runBlocking {
        insertReaped(TagData(name = "Gone"))
        val base = """{"rev":"v1","version":1,"tags":{"gone":{"deleted":true,"ts":1}},"order":[]}"""
        val cached = mock<VtodoCache> { onBlocking { getTagMetadata(any()) } doReturn base }
        val sync = TagMetadataSync(caldavDao, tagDataDao, mock<CaldavClientProvider>(), cached, preferences)
        val account = insertPrimaryAccount()
        preferences.set(TasksPreferences.metadataRev, "v1")
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn "v2"
            onBlocking { tagMetadata(any()) } doReturn """{"rev":"v2","version":1,"tags":{},"order":[]}"""
        }

        sync.pull(account, client, principal)

        assertEquals(false, dirty("gone"))
        assertEquals(true, reaped("gone"))
    }

    @Test
    fun `pull returns false for an unparseable payload so the sweep is skipped`() = runBlocking {
        val cached = mock<VtodoCache> {
            onBlocking { getTagMetadata(any()) } doReturn """{"rev":"old","version":1,"tags":{},"order":[]}"""
        }
        val sync = TagMetadataSync(caldavDao, tagDataDao, mock<CaldavClientProvider>(), cached, preferences)
        preferences.set(TasksPreferences.metadataRev, "old")
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn "v2"
            onBlocking { tagMetadata(any()) } doReturn "{{{ not json"
        }

        assertFalse(sync.pull(insertPrimaryAccount(), client, principal).applied)
    }

    @Test
    fun `pull returns true when it applies the store`() = runBlocking {
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn "v2"
            onBlocking { tagMetadata(any()) } doReturn """{"rev":"v2","version":1,"tags":{},"order":[]}"""
        }
        assertTrue(sync.pull(insertPrimaryAccount(), client, principal).applied)
    }

    @Test
    fun `pushDirty re-fetches when the handed-over store is stale`() = runBlocking {
        val account = insertPrimaryAccount()
        insertDirty(TagData(name = "New"))
        val stale = TagMetadataBlob.parse("""{"rev":"v1","version":1,"tags":{"work":{"name":"Work"}},"order":[]}""")!!
        var pushed: String? = null
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn "v2"
            onBlocking { tagMetadata(any()) } doReturn
                    """{"rev":"v2","version":1,"tags":{"work":{"name":"Work"},"gone":{"deleted":true,"ts":1}},"order":[]}"""
            onBlocking { pushTagMetadata(any(), any(), any()) } doAnswer { pushed = it.getArgument(1); true }
        }

        sync.pushDirty(account, client, principal, stale)

        verify(client).tagMetadata(any())
        assertTrue(pushed!!.contains("gone"))
    }

    @Test
    fun `pushDirty keeps a colour cleared while the push was in flight`() = runBlocking {
        insertDirty(TagData(name = "Work", color = -5))
        val account = insertPrimaryAccount()
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn null
            onBlocking { tagMetadata(any()) } doReturn null
            onBlocking { pushTagMetadata(any(), any(), any()) } doAnswer {
                runBlocking {
                    tagDataDao.editTag(
                        tagDataDao.getAll().single().remoteId!!,
                        name = "Work", color = 0, icon = null,
                        nameChanged = false, colorChanged = true, iconChanged = false,
                    )
                }
                true
            }
        }

        sync.pushDirty(account, client, principal)

        val work = tagDataDao.getAll().single()
        assertEquals(0, work.color)
        assertEquals(true, dirty("work"))
    }

    @Test
    fun `pushDirty after a takeover does not seed the previous store's inbound tombstones`() = runBlocking {
        val a = insertPrimaryAccount()
        tagDataDao.insert(TagData(name = "Old"))
        sync.applyRemote("""{"version":1,"tags":{"old":{"deleted":true,"ts":1}}}""")
        assertEquals(true, reaped("old"))

        val tasksOrg = caldavDao.getAccount(
            caldavDao.insert(CaldavAccount(accountType = CaldavAccount.TYPE_TASKS, uuid = "tasks-org"))
        )!!
        tagDataDao.insert(TagData(name = "Doomed"))
        var pushed: String? = null
        val client = mock<CaldavClient> {
            onBlocking { principal() } doReturn principal
            onBlocking { tagMetadataVersion(any()) } doReturn null
            onBlocking { tagMetadata(any()) } doReturn null
            onBlocking { pushTagMetadata(any(), any(), any()) } doAnswer { pushed = it.getArgument(1); true }
        }
        val provider = mock<CaldavClientProvider> { onBlocking { forAccount(any(), any()) } doReturn client }
        val editSync = TagMetadataSync(caldavDao, tagDataDao, provider, vtodoCache, preferences)

        editSync.deleteTag(tagDataDao.getAll().single { it.normalizedName == "doomed" })
        editSync.pushDirty(tasksOrg, client, principal)

        val seeded = TagMetadataBlob.parse(pushed)!!
        assertTrue("A's tombstone leaked into the new store", seeded.entryOf("old") is Entry.Live)
        assertTrue(seeded.entryOf("doomed") is Entry.Tomb)
        assertEquals(false, reaped("old"))
        assertEquals(a.id, preferences.get(TasksPreferences.metadataPrimaryAccount, 0L))
    }

    @Test
    fun `reapOrphaned is a no-op for a non-primary account`() = runBlocking {
        tagDataDao.insert(TagData(name = "Work"))
        sync.applyRemote("""{"version":1,"tags":{"work":{"deleted":true,"ts":1}}}""")
        val result = sync.reapOrphaned(CaldavAccount(id = 999))
        assertTrue(result.isEmpty())
        assertNotNull(tagDataDao.getAll().firstOrNull { it.name == "Work" })
    }

    @Test
    fun `pushDirty is a no-op for a non-primary account`() = runBlocking {
        insertDirty(TagData(name = "Home", color = -3))
        val client = mock<CaldavClient>()

        sync.pushDirty(CaldavAccount(id = 999), client, principal)

        verify(client, never()).pushTagMetadata(any(), any(), any())
        assertEquals(true, dirty("home"))
    }

    @Test
    fun `pull store-change teardown drops the previous store's reaped flags before adopting`() = runBlocking {
        tagDataDao.insert(TagData(name = "Old"))
        sync.applyRemote("""{"version":1,"tags":{"old":{"deleted":true,"ts":1}}}""")
        assertEquals(true, reaped("old"))

        val newPrimary = caldavDao.getAccount(caldavDao.insert(CaldavAccount()))!!
        preferences.set(TasksPreferences.metadataPrimaryAccount, newPrimary.id)
        val client = mock<CaldavClient> {
            onBlocking { tagMetadataVersion(any()) } doReturn null
            onBlocking { tagMetadata(any()) } doReturn null
            onBlocking { pushTagMetadata(any(), any(), any()) } doReturn true
        }

        sync.pull(newPrimary, client, principal)

        assertEquals(false, reaped("old"))
    }
}
