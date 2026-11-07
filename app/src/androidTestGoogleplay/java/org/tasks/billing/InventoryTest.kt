package org.tasks.billing

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.LocalBroadcastManager
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.injection.ApplicationScope
import org.tasks.injection.InjectingTestCase
import org.tasks.preferences.Preferences
import javax.inject.Inject

@HiltAndroidTest
class InventoryTest : InjectingTestCase() {

    @Inject lateinit var preferences: Preferences
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var playPurchases: PlayPurchases
    @Inject lateinit var entitlement: PurchaseEntitlement
    @Inject lateinit var caldavDao: CaldavDao
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    @Test
    fun hasTasksAccount() = runBlocking {
        caldavDao.insert(
            CaldavAccount(
                accountType = CaldavAccount.TYPE_TASKS,
                url = "https://caldav.tasks.org/calendars/",
            )
        )

        assertTrue(inventory().apply { updateTasksAccount() }.hasTasksAccount)
    }

    @Test
    fun hasTasksAccountWithCaldav() = runBlocking {
        caldavDao.insert(
            CaldavAccount(
                accountType = CaldavAccount.TYPE_CALDAV,
                url = "https://caldav.tasks.org/calendars/",
            )
        )

        assertTrue(inventory().apply { updateTasksAccount() }.hasTasksAccount)
    }

    private fun inventory() = Inventory(
        scope,
        preferences,
        playPurchases,
        entitlement,
        localBroadcastManager,
        caldavDao,
    )
}
