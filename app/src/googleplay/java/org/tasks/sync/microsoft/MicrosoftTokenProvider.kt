package org.tasks.sync.microsoft

import android.content.Context
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.data.entity.CaldavAccount
import timber.log.Timber
import javax.inject.Inject

class MicrosoftTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Volatile private var application: IMultipleAccountPublicClientApplication? = null

    private fun application(): IMultipleAccountPublicClientApplication =
        application ?: synchronized(this) {
            application ?: PublicClientApplication.createMultipleAccountPublicClientApplication(
                context,
                R.raw.microsoft_config,
            ).also { application = it }
        }

    suspend fun hasCredentials(account: CaldavAccount): Boolean = withContext(Dispatchers.IO) {
        try {
            application().accounts.any { it.username == account.username }
        } catch (e: Exception) {
            Timber.e(e)
            true
        }
    }

    fun getToken(account: CaldavAccount): String {
        val app = application()

        val result = try {
            val msalAccount = app.accounts.firstOrNull { it.username == account.username }
                ?: throw RuntimeException("No matching account found")

            val parameters = AcquireTokenSilentParameters.Builder()
                .withScopes(MicrosoftSignInViewModel.scopes)
                .forAccount(msalAccount)
                .fromAuthority(msalAccount.authority)
                .forceRefresh(true)
                .build()

            app.acquireTokenSilent(parameters)
        } catch (e: Exception) {
            Timber.e(e)
            throw RuntimeException("Authentication failed: ${e.message}")
        }
        return result.accessToken
    }
}
