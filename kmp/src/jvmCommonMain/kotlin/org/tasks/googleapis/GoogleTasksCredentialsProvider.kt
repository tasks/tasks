package org.tasks.googleapis

import org.tasks.data.entity.CaldavAccount

interface GoogleTasksCredentialsProvider : GoogleTasksAccountAccess {
    fun adapter(account: CaldavAccount): CredentialsAdapter
}
