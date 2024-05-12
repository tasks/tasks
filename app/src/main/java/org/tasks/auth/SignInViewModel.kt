package org.tasks.auth

import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.tasks.data.UUIDHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.ClientAuthentication.UnsupportedAuthenticationMethod
import net.openid.appauth.GrantTypeValues
import net.openid.appauth.TokenRequest
import org.tasks.R
import org.tasks.caldav.CaldavClientProvider
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.dao.CaldavDao
import org.tasks.security.KeyStoreEncryption
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val provider: CaldavClientProvider,
    private val caldavDao: CaldavDao,
    private val encryption: KeyStoreEncryption,
    private val debugConnectionBuilder: DebugConnectionBuilder
) : ViewModel() {
    val error = MutableLiveData<Throwable>()

    var authService: AuthorizationService? = null

    fun initializeAuthService(iss: String) {
        authService?.dispose()
        authService = AuthorizationService(iss, context, debugConnectionBuilder)
    }

    suspend fun handleResult(authService: AuthorizationService, intent: Intent): CaldavAccount? {
        val response = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)
        val authStateManager = authService.authStateManager

        if (response != null || ex != null) {
            authStateManager.updateAfterAuthorization(response, ex)
        }

        if (response?.authorizationCode != null) {
            authStateManager.updateAfterAuthorization(response, ex)
            exchangeAuthorizationCode(authService, response)
        }

        ex?.let {
            error.value = it
            return null
        }

        return authStateManager.current
                .takeIf { it.isAuthorized }
                ?.let { setupAccount(authService) }
    }

    suspend fun setupAccount(authService: AuthorizationService): CaldavAccount? {
        val auth = authService.authStateManager.current
        val tokenString = auth.accessToken ?: return null
        val idToken = auth.idToken?.let { IdToken(it) } ?: return null
        val username = "${authService.iss}_${idToken.sub}"
        try {
            val homeSet = provider
                    .forUrl(
                            context.getString(R.string.tasks_caldav_url),
                            username,
                            tokenString
                    )
                    .homeSet(username, tokenString)
            val password = encryption.encrypt(tokenString)
            return caldavDao.getAccount(CaldavAccount.TYPE_TASKS, username)
                    ?.let {
                        it.copy(error = null, password = password)
                            .also { caldavDao.update(it) }
                    }
                    ?: CaldavAccount(
                        accountType = CaldavAccount.TYPE_TASKS,
                        uuid = UUIDHelper.newUUID(),
                        username = username,
                        password = password,
                        url = homeSet,
                        name = idToken.email ?: idToken.login,
                    ).let {
                        it.copy(id = caldavDao.insert(it))
                    }
        } catch (e: Exception) {
            error.postValue(e)
        }
        return null
    }

    private suspend fun exchangeAuthorizationCode(
            authService: AuthorizationService,
            authorizationResponse: AuthorizationResponse
    ) {
        val authStateManager = authService.authStateManager
        val request = if (authService.isGitHub) {
            authorizationResponse.createGithubTokenRequest()
        } else {
            authorizationResponse.createTokenExchangeRequest()
        }
        val clientAuthentication = try {
            authStateManager.current.clientAuthentication
        } catch (ex: UnsupportedAuthenticationMethod) {
            throw ex
        }
        try {
            authService.performTokenRequest(request, clientAuthentication)?.let {
                authStateManager.updateAfterTokenResponse(it, null)
                if (authStateManager.current.isAuthorized) {
                    Timber.d("Authorization successful")
                }
            }
        } catch (e: AuthorizationException) {
            Timber.e(e)
            authStateManager.updateAfterTokenResponse(null, e)
        }
    }

    override fun onCleared() {
        authService?.dispose()
    }

    companion object {
        fun AuthorizationResponse.createGithubTokenRequest(): TokenRequest {
            checkNotNull(authorizationCode) { "authorizationCode not available for exchange request" }
            return TokenRequest
                    .Builder(request.configuration, request.clientId)
                    .setGrantType(GrantTypeValues.AUTHORIZATION_CODE)
                    .setRedirectUri(request.redirectUri)
                    .setCodeVerifier(request.codeVerifier)
                    .setAuthorizationCode(authorizationCode)
                    .setAdditionalParameters(emptyMap())
                    .build()
        }
    }
}