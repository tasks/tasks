package org.tasks.sync.microsoft

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import net.openid.appauth.AuthState
import org.tasks.data.entity.CaldavAccount
import org.tasks.security.KeyStoreEncryption
import javax.inject.Inject

class MicrosoftTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryption: KeyStoreEncryption,
) {
    suspend fun getToken(account: CaldavAccount): String {
        val authState = encryption.decrypt(account.password)?.let { AuthState.jsonDeserialize(it) }
            ?: throw RuntimeException("Missing credentials")
        if (authState.needsTokenRefresh) {
            val (token, ex) = context.requestTokenRefresh(authState)
            authState.update(token, ex)
            if (authState.isAuthorized) {
                account.password = encryption.encrypt(authState.jsonSerializeString())
            }
        }
        if (!authState.isAuthorized) {
            throw RuntimeException("Needs authentication")
        }
        return authState.accessToken!!
    }
}