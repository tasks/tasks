package org.tasks.fcm

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.tasks.caldav.CaldavClientProvider
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount

private const val TAG = "PushTokenManager"

class PushTokenManager(
    private val tokenProvider: FcmTokenProvider,
    private val caldavDao: CaldavDao,
    private val caldavClientProvider: CaldavClientProvider,
    private val scope: CoroutineScope,
) {
    fun registerTokenForAccount(account: CaldavAccount) {
        scope.launch {
            try {
                val token = tokenProvider.getToken() ?: return@launch
                caldavClientProvider.forTasksAccount(account)
                    .registerPushToken(token)
            } catch (e: Exception) {
                Logger.e(e, tag = TAG) { "Failed to register push token" }
            }
        }
    }

    fun registerTokenForAllAccounts() {
        scope.launch {
            val token = tokenProvider.getToken() ?: return@launch
            for (account in caldavDao.getAccounts(CaldavAccount.TYPE_TASKS)) {
                try {
                    caldavClientProvider.forTasksAccount(account)
                        .registerPushToken(token)
                } catch (e: Exception) {
                    Logger.e(e, tag = TAG) { "Failed to register push token" }
                }
            }
        }
    }

    suspend fun unregisterToken(account: CaldavAccount) {
        try {
            val token = tokenProvider.getToken() ?: return
            caldavClientProvider.forTasksAccount(account)
                .unregisterPushToken(token)
        } catch (e: Exception) {
            Logger.e(e, tag = TAG) { "Failed to unregister push token" }
        }
    }
}
