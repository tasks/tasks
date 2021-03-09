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
import org.tasks.data.CaldavCalendar.Companion.ACCESS_READ_WRITE
import org.tasks.data.CaldavCalendar.Companion.INVITE_UNKNOWN
import org.tasks.data.CaldavDao
import org.tasks.data.Principal
import org.tasks.data.PrincipalDao
import org.tasks.sync.SyncAdapters
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CaldavCalendarViewModel @Inject constructor(
    private val provider: CaldavClientProvider,
    private val caldavDao: CaldavDao,
    private val principalDao: PrincipalDao,
    private val taskDeleter: TaskDeleter,
    private val syncAdapters: SyncAdapters,
) : ViewModel() {
    val error = MutableLiveData<Throwable?>()
    val inFlight = MutableLiveData(false)
    val finish = MutableLiveData<Intent>()
    var ignoreFinish = false

    suspend fun createCalendar(
        caldavAccount: CaldavAccount,
        name: String,
        color: Int,
        icon: Int
    ): CaldavCalendar? =
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
            if (!ignoreFinish) {
                finish.value = Intent().putExtra(MainActivity.OPEN_FILTER, CaldavFilter(calendar))
            }
            calendar
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

    suspend fun addUser(
        account: CaldavAccount,
        list: CaldavCalendar,
        input: String
    ) = doRequest {
        val href = if (account.serverType == CaldavAccount.SERVER_OWNCLOUD)
            "principal:principals/users/$input"
        else
            "mailto:$input"
        withContext(Dispatchers.IO) {
            provider.forAccount(account, list.url!!).share(account, href)
        }
        principalDao.insert(Principal().apply {
            this.list = list.id
            principal = href
            inviteStatus = INVITE_UNKNOWN
            access = ACCESS_READ_WRITE
        })
        syncAdapters.sync(true)
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
                Timber.e(e)
                error.value = e
                return@withContext null
            } finally {
                inFlight.value = false
            }
        }
}