package org.tasks.opentasks

import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.R
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.CaldavDao
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.preferences.Preferences
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class OpenTasksSynchronizerTest : InjectingTestCase() {
    @Inject lateinit var openTaskDao: TestOpenTaskDao
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var synchronizer: OpenTasksSynchronizer
    @Inject lateinit var preferences: Preferences

    @Before
    override fun setUp() {
        super.setUp()

        openTaskDao.reset()
        preferences.setBoolean(R.string.p_debug_pro, true)
    }

    @Test
    fun createNewAccounts() = runBlocking {
        openTaskDao.insertList()

        synchronizer.sync()

        val accounts = caldavDao.getAccounts()
        assertEquals(1, accounts.size)
        with(accounts[0]) {
            assertEquals("bitfire.at.davdroid:test_account", uuid)
            assertEquals("test_account", name)
            assertEquals(TYPE_OPENTASKS, accountType)
        }
    }

    @Test
    fun cantSyncWithoutPro() = runBlocking {
        preferences.setBoolean(R.string.p_debug_pro, false)
        openTaskDao.insertList()

        synchronizer.sync()

        assertEquals(
                context.getString(R.string.requires_pro_subscription),
                caldavDao.getAccounts()[0].error
        )
    }

    @Test
    fun deleteRemovedAccounts() = runBlocking {
        caldavDao.insert(CaldavAccount().apply {
            uuid = "bitfire.at.davdroid:test_account"
            accountType = TYPE_OPENTASKS
        })

        synchronizer.sync()

        assertTrue(caldavDao.getAccounts().isEmpty())
    }

    @Test
    fun createNewLists() = runBlocking {
        openTaskDao.insertList()

        synchronizer.sync()

        val lists = caldavDao.getCalendarsByAccount("bitfire.at.davdroid:test_account")
        assertEquals(1, lists.size)
        with(lists[0]) {
            assertEquals(name, "default_list")
        }
    }
}