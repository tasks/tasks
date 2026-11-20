package org.tasks.auth

import org.tasks.compose.accounts.Platform
import org.tasks.data.dao.CaldavDao

class TasksOrgAccountSignInHandler(
    private val delegate: SignInHandler,
    private val caldavDao: CaldavDao,
) : SignInHandler {

    override suspend fun signIn(
        platform: Platform,
        provider: OAuthProvider?,
        loginHint: String?,
        onAuthorized: () -> Unit,
    ) {
        if (platform != Platform.TASKS_ORG) {
            delegate.signIn(platform, provider, loginHint, onAuthorized)
            return
        }
        val target = caldavDao.resolveTasksOrgSignIn(provider, loginHint)
        delegate.signIn(
            platform = platform,
            provider = target.provider,
            loginHint = target.loginHint,
            onAuthorized = onAuthorized,
        )
    }
}
