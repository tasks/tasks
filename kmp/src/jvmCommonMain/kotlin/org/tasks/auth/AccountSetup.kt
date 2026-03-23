package org.tasks.auth

import org.tasks.caldav.CaldavClientProvider
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.security.KeyStoreEncryption

suspend fun setupTasksAccount(
    oauthResult: OAuthResult,
    issuer: String,
    caldavUrl: String,
    caldavDao: CaldavDao,
    encryption: KeyStoreEncryption,
    provider: CaldavClientProvider,
): CaldavAccount {
    val username = "${issuer}_${oauthResult.idToken.sub}"
    val tokenString = oauthResult.accessToken
    val password = encryption.encrypt(tokenString)
    return caldavDao.getAccount(CaldavAccount.TYPE_TASKS, username)
        ?.let {
            it.copy(error = null, password = password)
                .also { updated -> caldavDao.update(updated) }
        }
        ?: run {
            val homeSet = provider
                .forUrl(caldavUrl, username, tokenString)
                .homeSet(username, tokenString)
            CaldavAccount(
                accountType = CaldavAccount.TYPE_TASKS,
                uuid = UUIDHelper.newUUID(),
                username = username,
                password = password,
                url = homeSet,
                name = oauthResult.idToken.email ?: oauthResult.idToken.login,
                serverType = CaldavAccount.SERVER_TASKS,
            ).let {
                it.copy(id = caldavDao.insert(it))
            }
        }
}
