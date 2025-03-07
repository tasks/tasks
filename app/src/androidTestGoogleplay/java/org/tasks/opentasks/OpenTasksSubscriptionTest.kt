package org.tasks.opentasks

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.R

@HiltAndroidTest
class OpenTasksSubscriptionTest : OpenTasksTest() {
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
}