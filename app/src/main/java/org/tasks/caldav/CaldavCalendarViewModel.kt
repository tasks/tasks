package org.tasks.caldav

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.helper.UUIDHelper
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavDao
import org.tasks.data.Principal
import org.tasks.data.PrincipalDao
import javax.inject.Inject

@HiltViewModel
class CaldavCalendarViewModel @Inject constructor(
    private val provider: CaldavClientProvider,
    private val caldavDao: CaldavDao,
    private val principalDao: PrincipalDao,
    private val taskDeleter: TaskDeleter,
) : ViewModel() {
    val error = MutableLiveData<Throwable?>()
    val inFlight = MutableLiveData(false)
    val finish = MutableLiveData<Intent>()

    suspend fun createCalendar(caldavAccount: CaldavAccount, name: String, color: Int, icon: Int) =
        doRequest {
            val url = withContext(Dispatchers.IO) {
                provider.forAccount(caldavAccount).makeCollection(name, color)
            }
            val calendar = CaldavCalendar().apply {
                uuid = UUIDHelper.newUUID()
                account = caldavAccount.uuid
                this.url = url
                this.name = name
                this.color = color
                setIcon(icon)
                caldavDao.insert(this)
            }
            finish.value = Intent().putExtra(MainActivity.OPEN_FILTER, CaldavFilter(calendar))
        }

    suspend fun updateCalendar(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        name: String,
        color: Int,
        icon: Int
    ) =
        doRequest {
            withContext(Dispatchers.IO) {
                provider.forAccount(account, calendar.url!!).updateCollection(name, color)
            }
            calendar.apply {
                this.name = name
                this.color = color
                setIcon(icon)
                caldavDao.update(this)
            }
            finish.value = Intent(TaskListFragment.ACTION_RELOAD)
                .putExtra(MainActivity.OPEN_FILTER, CaldavFilter(calendar))
        }

    suspend fun deleteCalendar(account: CaldavAccount, calendar: CaldavCalendar) =
        doRequest {
            withContext(Dispatchers.IO) {
                provider.forAccount(account, calendar.url!!).deleteCollection()
            }
            taskDeleter.delete(calendar)
            finish.value = Intent(TaskListFragment.ACTION_DELETED)
        }

    suspend fun removeUser(account: CaldavAccount, list: CaldavCalendar, principal: Principal) =
        doRequest {
            withContext(Dispatchers.IO) {
                provider.forAccount(account).removePrincipal(account, list, principal)
            }
            principalDao.delete(principal)
        }

    private suspend fun <T> doRequest(action: suspend () -> T): T? =
        withContext(NonCancellable) {
            if (inFlight.value == true) {
                return@withContext null
            }
            inFlight.value = true
            try {
                return@withContext action()
            } catch (e: Exception) {
                error.value = e
                return@withContext null
            } finally {
                inFlight.value = false
            }
        }
}