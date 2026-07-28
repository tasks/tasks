package org.tasks.sync.microsoft

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
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
            ?: throw RuntimeException("Missing credentials")
        if (authState.needsTokenRefresh) {
            withContext(NonCancellable) {
                val (token, ex) = context.requestTokenRefresh(authState)
                authState.update(token, ex)
                if (authState.isAuthorized) {
                    encryption.encrypt(authState.jsonSerializeString())?.let { encrypted ->
                        account.password = encrypted
                        account.id.takeIf { it != 0L }?.let { caldavDao.setPassword(it, encrypted) }
                    }
                }
            }
        }
        if (!authState.isAuthorized) {
            throw RuntimeException("Needs authentication")
        }
        return authState.accessToken!!
    }
}