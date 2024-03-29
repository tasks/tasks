package org.tasks.data

import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class GoogleTaskListDaoTest : InjectingTestCase() {
    @Inject lateinit var googleTaskListDao: GoogleTaskListDao
    @Inject lateinit var caldavDao: CaldavDao

    @Test
    fun noResultsForEmptyAccount() = runBlocking {
        val account = CaldavAccount().apply {
            uuid = "user@gmail.com"
            username = "user@gmail.com"
        }
        caldavDao.insert(account)

        assertTrue(googleTaskListDao.getGoogleTaskFilters(account.username!!).isEmpty())
    }
}