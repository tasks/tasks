package org.tasks.caldav

import androidx.test.annotation.UiThreadTest
import org.tasks.data.UUIDHelper
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.jetbrains.compose.resources.getString
import org.tasks.data.entity.CaldavAccount
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.requires_pro_subscription
import javax.inject.Inject

@HiltAndroidTest
class CaldavSubscriptionTest : CaldavTest() {

    @Test
    @UiThreadTest
    fun cantSyncWithoutPro() = runBlocking {
        account = CaldavAccount(uuid = UUIDHelper.newUUID())
            .let { it.copy(id = caldavDao.insert(it)) }

        synchronizer.sync(account, hasPro = false)

        assertEquals(
                getString(Res.string.requires_pro_subscription),
                caldavDao.getAccountByUuid(account.uuid!!)?.error
        )
    }
}