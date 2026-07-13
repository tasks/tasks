package org.tasks.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.tasks.DatabaseTest
import org.tasks.analytics.Reporting
import org.tasks.billing.PurchaseState
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.metadata.TagMetadataSync
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.TagData
import org.tasks.preferences.TasksPreferences
import org.tasks.sync.SyncAdapters

@OptIn(ExperimentalCoroutinesApi::class)
class TagSettingsViewModelTest : DatabaseTest() {

    private val tagDataDao = db.tagDataDao()
    private val caldavDao = db.caldavDao()
    private val preferences = TasksPreferences(InMemoryDataStore())
    private val tagMetadataSync =
        TagMetadataSync(caldavDao, tagDataDao, mock(), mock(), preferences)

    private class InMemoryDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(Dispatchers.Unconfined)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(tagData: TagData) = TagSettingsViewModel(
        tagDataDao = tagDataDao,
        refreshBroadcaster = mock<RefreshBroadcaster>(),
        reporting = mock<Reporting>(),
        purchaseState = mock<PurchaseState>(),
        tagMetadataSync = tagMetadataSync,
        syncAdapters = mock<SyncAdapters>(),
        isDark = false,
        tagData = tagData,
    )

    private suspend fun TagSettingsViewModel.saveAndAwait(onComplete: (TagData) -> Unit = {}) {
        save(onComplete = onComplete)
        withTimeout(5_000) { viewState.first { !it.isLoading } }
    }

    private suspend fun makeMetadataPrimary() {
        val account = caldavDao.getAccount(caldavDao.insert(CaldavAccount()))!!
        preferences.set(TasksPreferences.metadataPrimaryAccount, account.id!!)
        preferences.set(TasksPreferences.metadataStoreAccount, account.id!!)
    }

    @Test
    fun `create uses a fresh identity, not the dialog placeholder`() = runBlocking {
        val placeholder = TagData()
        val vm = viewModel(placeholder)

        var created: TagData? = null
        vm.setName("Work")
        vm.saveAndAwait { created = it }

        assertNotNull(created)
        assertNotEquals(placeholder.remoteId, created!!.remoteId)
        assertEquals(listOf("Work"), tagDataDao.getAll().map { it.name })
        assertFalse(vm.viewState.value.isNew)
    }

    @Test
    fun `rename after create in the same instance renames in place, no duplicate`() = runBlocking {
        makeMetadataPrimary()
        val vm = viewModel(TagData())

        vm.setName("Work")
        vm.saveAndAwait()
        val createdUuid = tagDataDao.getAll().single().remoteId

        vm.setName("Job")
        vm.saveAndAwait()

        val all = tagDataDao.getAll()
        assertEquals(listOf("Job"), all.map { it.name })
        assertEquals(createdUuid, all.single().remoteId)
        assertEquals(listOf("work"), tagDataDao.getTombstoneKeys())
    }
}
