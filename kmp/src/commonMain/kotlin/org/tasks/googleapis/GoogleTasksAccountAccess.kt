package org.tasks.googleapis

import org.tasks.auth.isUnauthorized
import org.tasks.data.entity.CaldavAccount

interface GoogleTasksAccountAccess {
    fun canAccess(account: CaldavAccount): Boolean
    fun needsSignIn(account: CaldavAccount): Boolean
}

object OAuthTokenAccountAccess : GoogleTasksAccountAccess {
    override fun canAccess(account: CaldavAccount) = !account.password.isNullOrBlank()

    override fun needsSignIn(account: CaldavAccount) = account.isUnauthorized()
}
