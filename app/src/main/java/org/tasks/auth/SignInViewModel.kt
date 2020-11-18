package org.tasks.auth

import android.content.Context
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.todoroo.astrid.helper.UUIDHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.ClientAuthentication.UnsupportedAuthenticationMethod
import org.tasks.R
import org.tasks.caldav.CaldavClientProvider
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavDao
import timber.log.Timber

class SignInViewModel @ViewModelInject constructor(
        @ApplicationContext private val context: Context,
        private val authStateManager: AuthStateManager,
        private val authorizationService: AuthorizationService,
        private val provider: CaldavClientProvider,
        private val caldavDao: CaldavDao
) : ViewModel() {
    suspend fun handleResult(intent: Intent): CaldavAccount? {
        val response = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)

        if (response != null || ex != null) {
            authStateManager.updateAfterAuthorization(response, ex)
        }

        if (response?.authorizationCode != null) {
            authStateManager.updateAfterAuthorization(response, ex)
            exchangeAuthorizationCode(response)
        }
        val auth = authStateManager.current
        if (!auth.isAuthorized) {
            return null
        }
        val tokenString = auth.idToken ?: return null
        val idToken = IdToken(tokenString)
        val username = "google_${idToken.sub}"
        val homeSet = provider
                .forUrl(
                        "${context.getString(R.string.tasks_caldav_url)}/google_login",
                        token = tokenString
                )
                .setForeground()
                .homeSet(token = tokenString)
        return caldavDao.getAccount(CaldavAccount.TYPE_TASKS, username)
                ?.apply {
                    error = null
                    caldavDao.update(this)
                }
                ?: CaldavAccount().apply {
                    accountType = CaldavAccount.TYPE_TASKS
                    uuid = UUIDHelper.newUUID()
                    url = homeSet
                    this.username = username
                    name = idToken.email
                    caldavDao.insert(this)
                }
    }

    private suspend fun exchangeAuthorizationCode(authorizationResponse: AuthorizationResponse) {
        val request = authorizationResponse.createTokenExchangeRequest()
        val clientAuthentication = try {
            authStateManager.current.clientAuthentication
        } catch (ex: UnsupportedAuthenticationMethod) {
            throw ex
        }
        try {
            authorizationService.performTokenRequest(request, clientAuthentication)?.let {
                authStateManager.updateAfterTokenResponse(it, null)
                if (authStateManager.current.isAuthorized) {
                    Timber.d("Authorization successful")
                }
            }
        } catch (e: AuthorizationException) {
            authStateManager.updateAfterTokenResponse(null, e)
        }
    }
}