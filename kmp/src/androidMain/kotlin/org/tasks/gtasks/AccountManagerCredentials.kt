package org.tasks.gtasks

import com.google.api.client.http.HttpRequest
import com.google.api.services.tasks.TasksScopes
import org.tasks.data.entity.CaldavAccount
import org.tasks.googleapis.CredentialsAdapter
import org.tasks.googleapis.GoogleTasksCredentialsProvider

class AccountManagerCredentials(
    private val googleAccountManager: GoogleAccountManager,
) : GoogleTasksCredentialsProvider {

    override fun canAccess(account: CaldavAccount) =
        googleAccountManager.getAccount(account.username) != null

    override fun needsSignIn(account: CaldavAccount) = false

    override fun adapter(account: CaldavAccount): CredentialsAdapter =
        adapter(account.username!!, TasksScopes.TASKS)

    fun adapter(accountName: String, scope: String): CredentialsAdapter =
        AccountManagerCredentialsAdapter(googleAccountManager, accountName, scope)
}

private class AccountManagerCredentialsAdapter(
    private val googleAccountManager: GoogleAccountManager,
    private val accountName: String,
    private val scope: String,
) : CredentialsAdapter {

    @Volatile private var token: String? = null

    override fun initialize(request: HttpRequest) {
        token?.let { request.headers.authorization = "Bearer $it" }
    }

    override suspend fun checkToken() {
        if (token == null) {
            token = googleAccountManager.getAccessToken(accountName, scope)
        }
    }

    override suspend fun invalidateToken(): Boolean {
        googleAccountManager.invalidateToken(token)
        token = null
        return true
    }
}
