package org.tasks.googleapis

import org.tasks.data.dao.tasksOrgAccount
import okhttp3.Credentials
import org.tasks.data.dao.CaldavDao
import org.tasks.security.KeyStoreEncryption

class ProxyAuthProvider(
    private val caldavDao: CaldavDao,
    private val encryption: KeyStoreEncryption,
    private val jwtProvider: suspend () -> String?,
) {
    suspend fun getAuthHeader(): String? {
        jwtProvider()?.let { return "Bearer $it" }

        val tasksAccount = caldavDao.tasksOrgAccount() ?: return null
        val username = tasksAccount.username ?: return null
        val token = tasksAccount.password
            ?.let { encryption.decrypt(it) }
            ?: return null
        return Credentials.basic(username, token, Charsets.UTF_8)
    }
}
