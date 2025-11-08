package org.tasks.data

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.injection.InjectingTestCase
import javax.inject.Inject

@HiltAndroidTest
class CaldavDaoExtensionsTest : InjectingTestCase() {
    @Inject lateinit var caldavDao: CaldavDao

    @Test
    fun getLocalListCreatesAccountIfNeeded() = runBlocking {
        withTimeout(5000L) {
            assertTrue(caldavDao.getAccounts().isEmpty())
            caldavDao.getLocalList()
            assertTrue(caldavDao.getAccounts(CaldavAccount.TYPE_LOCAL).isNotEmpty())
        }
    }
}
