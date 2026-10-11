package org.tasks.opentasks

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.resources.getString
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.subscription_required

@HiltAndroidTest
class OpenTasksSubscriptionTest : OpenTasksTest() {
    @Test
    fun cantSyncWithoutPro() = runBlocking {
        openTaskDao.insertList()

        synchronizer.sync(hasPro = false)

        assertEquals(
                getString(Res.string.subscription_required),
                caldavDao.getAccounts()[0].error
        )
    }
}