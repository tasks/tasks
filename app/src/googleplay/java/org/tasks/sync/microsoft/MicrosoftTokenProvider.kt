package org.tasks.sync.microsoft

import android.content.Context
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.PublicClientApplication
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.data.entity.CaldavAccount
import timber.log.Timber
import javax.inject.Inject

class MicrosoftTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getToken(account: CaldavAccount): String {
        val app = PublicClientApplication.createMultipleAccountPublicClientApplication(
            context,
            R.raw.microsoft_config
        )

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
