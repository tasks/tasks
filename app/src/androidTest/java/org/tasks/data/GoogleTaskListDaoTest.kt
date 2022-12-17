package org.tasks.data

import com.natpryce.makeiteasy.MakeItEasy.with
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.makers.GoogleTaskListMaker.ACCOUNT
import org.tasks.makers.GoogleTaskListMaker.REMOTE_ID
import org.tasks.makers.GoogleTaskListMaker.newGoogleTaskList
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

    @Test
    fun findListWithNullAccount() = runBlocking {
        val list = newGoogleTaskList(with(REMOTE_ID, "1234"), with(ACCOUNT, null as String?))
        list.id = googleTaskListDao.insert(list)

        assertEquals(list, googleTaskListDao.findExistingList("1234"))
    }

    @Test
    fun findListWithEmptyAccount() = runBlocking {
        val list = newGoogleTaskList(with(REMOTE_ID, "1234"), with(ACCOUNT, ""))
        list.id = googleTaskListDao.insert(list)

        assertEquals(list, googleTaskListDao.findExistingList("1234"))
    }

    @Test
    fun ignoreListWithAccount() = runBlocking {
        val list = newGoogleTaskList(with(REMOTE_ID, "1234"), with(ACCOUNT, "user@gmail.com"))
        googleTaskListDao.insert(list)

        assertNull(googleTaskListDao.findExistingList("1234"))
    }
}