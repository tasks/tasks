package org.tasks.auth

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.tasks.analytics.Analytics
import org.tasks.data.dao.CaldavDao
import org.tasks.gtasks.GoogleAccountManager
import org.tasks.gtasks.GoogleAccountPicker
import org.tasks.ui.DisplayableException
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cannot_access_account

class DeviceGoogleTasksSignIn(
    private val googleAccountPicker: GoogleAccountPicker,
    private val caldavDao: CaldavDao,
    private val analytics: Analytics,
) : PlatformSignIn {

    override suspend fun signIn(loginHint: String?, onAuthorized: () -> Unit) {
        val accountName = try {
            googleAccountPicker.pick(loginHint)
        } catch (e: GoogleAccountManager.AccountNotFoundException) {
            throw DisplayableException(Res.string.cannot_access_account)
        }
        onAuthorized()
        withContext(NonCancellable) {
            setupGoogleTasksAccount(accountName, caldavDao, analytics)
        }
    }
}
