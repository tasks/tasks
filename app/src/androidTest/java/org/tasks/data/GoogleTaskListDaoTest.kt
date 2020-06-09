package org.tasks.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.natpryce.makeiteasy.MakeItEasy.with
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.TestComponent
import org.tasks.makers.GoogleTaskListMaker.ACCOUNT
import org.tasks.makers.GoogleTaskListMaker.REMOTE_ID
import org.tasks.makers.GoogleTaskListMaker.newGoogleTaskList
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class GoogleTaskListDaoTest : InjectingTestCase() {

    @Inject lateinit var googleTaskListDao: GoogleTaskListDao

    @Test
    fun noResultsForEmptyAccount() {
        val account = GoogleTaskAccount()
        account.account = "user@gmail.com"
        googleTaskListDao.insert(account)

        assertTrue(googleTaskListDao.getGoogleTaskFilters(account.account!!).isEmpty())
    }

    @Test
    fun findListWithNullAccount() {
        val list = newGoogleTaskList(with(REMOTE_ID, "1234"), with(ACCOUNT, null as String?))
        list.id = googleTaskListDao.insert(list)

        assertEquals(list, googleTaskListDao.findExistingList("1234"))
    }

    @Test
    fun findListWithEmptyAccount() {
        val list = newGoogleTaskList(with(REMOTE_ID, "1234"), with(ACCOUNT, ""))
        list.id = googleTaskListDao.insert(list)

        assertEquals(list, googleTaskListDao.findExistingList("1234"))
    }

    @Test
    fun ignoreListWithAccount() {
        val list = newGoogleTaskList(with(REMOTE_ID, "1234"), with(ACCOUNT, "user@gmail.com"))
        googleTaskListDao.insert(list)

        assertNull(googleTaskListDao.findExistingList("1234"))
    }

    override fun inject(component: TestComponent) = component.inject(this)
}