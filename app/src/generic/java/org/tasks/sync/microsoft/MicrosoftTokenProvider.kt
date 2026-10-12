package org.tasks.sync.microsoft

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.http.UnauthorizedException
import org.tasks.security.KeyStoreEncryption
import javax.inject.Inject

class MicrosoftTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryption: KeyStoreEncryption,
    private val caldavDao: CaldavDao,
) {
    fun hasCredentials(account: CaldavAccount): Boolean = !account.password.isNullOrBlank()

    suspend fun getToken(account: CaldavAccount): String {
        val authState = encryption.decrypt(account.password)?.let { AuthState.jsonDeserialize(it) }
            ?: throw UnauthorizedException("Missing credentials")
        if (authState.needsTokenRefresh) {
            val failure = withContext(NonCancellable) {
                val (token, ex) = context.requestTokenRefresh(authState)
                authState.update(token, ex)
                if (authState.isAuthorized) {
                    encryption.encrypt(authState.jsonSerializeString())?.let { encrypted ->
                        account.password = encrypted
                        account.id.takeIf { it != 0L }?.let { caldavDao.setPassword(it, encrypted) }
                    }
                }
                ex
            }
            if (failure != null && !authState.isAuthorized) {
                throw failure.toTokenException()
            }
        }
        if (!authState.isAuthorized) {
            throw UnauthorizedException("Needs authentication")
        }
        return authState.accessToken ?: throw UnauthorizedException("Missing access token")
    }
}