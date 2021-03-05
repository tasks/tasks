package org.tasks.caldav

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.data.Principal
import org.tasks.data.PrincipalDao
import javax.inject.Inject

@HiltViewModel
class CaldavCalendarViewModel @Inject constructor(
    private val provider: CaldavClientProvider,
    private val principalDao: PrincipalDao
) : ViewModel() {
    val error = MutableLiveData<Throwable?>()
    val inFlight = MutableLiveData(false)

    suspend fun remove(account: CaldavAccount, list: CaldavCalendar, principal: Principal) =
        withContext(NonCancellable) {
            if (inFlight.value == true) {
                return@withContext
            }
            inFlight.value = true
            try {
                provider.forAccount(account).removePrincipal(account, list, principal)
                principalDao.delete(principal)
            } catch (e: Exception) {
                error.value = e
            } finally {
                inFlight.value = false
            }
        }
}