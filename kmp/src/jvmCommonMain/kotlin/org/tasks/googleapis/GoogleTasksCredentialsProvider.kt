package org.tasks.googleapis

import org.tasks.data.entity.CaldavAccount

interface GoogleTasksCredentialsProvider {
    fun canAccess(account: CaldavAccount): Boolean
    fun needsSignIn(account: CaldavAccount): Boolean
    fun adapter(account: CaldavAccount): CredentialsAdapter
}
