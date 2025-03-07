package org.tasks.caldav

import androidx.test.annotation.UiThreadTest
import org.tasks.data.UUIDHelper
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.data.entity.CaldavAccount
import javax.inject.Inject

@HiltAndroidTest
class CaldavSubscriptionTest : CaldavTest() {
    @Inject lateinit var inventory: Inventory

    @Test
    @UiThreadTest
    fun cantSyncWithoutPro() = runBlocking {
        preferences.setBoolean(R.string.p_debug_pro, false)
        inventory.clear()
        inventory.add(emptyList())

        account = CaldavAccount(uuid = UUIDHelper.newUUID())
            .let { it.copy(id = caldavDao.insert(it)) }

        synchronizer.sync(account)

        assertEquals(
                context.getString(R.string.requires_pro_subscription),
                caldavDao.getAccountByUuid(account.uuid!!)?.error
        )
    }
}