package org.tasks.fcm

import org.tasks.analytics.Firebase
import org.tasks.caldav.CaldavClientProvider
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.injection.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushTokenManager @Inject constructor(
    private val firebase: Firebase,
    private val caldavDao: CaldavDao,
    private val caldavClientProvider: CaldavClientProvider,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun registerTokenForAccount(account: CaldavAccount) {
        scope.launch {
            try {
                val token = firebase.getToken() ?: return@launch
                caldavClientProvider.forTasksAccount(account)
                    .registerPushToken(token)
            } catch (e: Exception) {
                Timber.e(e, "Failed to register push token")
            }
        }
    }

    fun registerTokenForAllAccounts() {
        scope.launch {
            val token = firebase.getToken() ?: return@launch
            for (account in caldavDao.getAccounts(CaldavAccount.TYPE_TASKS)) {
                try {
                    caldavClientProvider.forTasksAccount(account)
                        .registerPushToken(token)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to register push token")
                }
            }
        }
    }

    suspend fun unregisterToken(account: CaldavAccount) {
        try {
            val token = firebase.getToken() ?: return
            caldavClientProvider.forTasksAccount(account)
                .unregisterPushToken(token)
        } catch (e: Exception) {
            Timber.e(e, "Failed to unregister push token")
        }
    }
}
