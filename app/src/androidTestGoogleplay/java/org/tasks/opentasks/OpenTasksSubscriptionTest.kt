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
        openTaskDao.insertList()

        synchronizer.sync(hasPro = false)

        assertEquals(
                context.getString(R.string.requires_pro_subscription),
                caldavDao.getAccounts()[0].error
        )
    }
}