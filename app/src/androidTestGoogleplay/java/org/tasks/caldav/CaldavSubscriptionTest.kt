package org.tasks.caldav

import com.todoroo.astrid.helper.UUIDHelper
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.R
import org.tasks.data.CaldavAccount
import org.tasks.injection.ProductionModule

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class CaldavSubscriptionTest : CaldavTest() {
    @Test
    fun cantSyncWithoutPro() = runBlocking {
        preferences.setBoolean(R.string.p_debug_pro, false)
        account = CaldavAccount().apply {
            uuid = UUIDHelper.newUUID()
            id = caldavDao.insert(this)
        }

        synchronizer.sync(account)

        assertEquals(
                context.getString(R.string.requires_pro_subscription),
                caldavDao.getAccountByUuid(account.uuid!!)?.error
        )
    }
}