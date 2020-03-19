package org.tasks.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.todoroo.andlib.utility.DateUtilities.now
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.TestComponent
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class GoogleTaskListDaoTest : InjectingTestCase() {

    @Inject lateinit var googleTaskListDao: GoogleTaskListDao

    @Test
    fun noResultsForEmptyAccount() {
        val account = GoogleTaskAccount()
        account.account = "user@gmail.com"
        googleTaskListDao.insert(account)

        assertTrue(googleTaskListDao.getGoogleTaskFilters(account.account, now()).isEmpty())
    }

    override fun inject(component: TestComponent) = component.inject(this)
}