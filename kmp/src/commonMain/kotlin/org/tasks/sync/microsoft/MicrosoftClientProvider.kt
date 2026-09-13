package org.tasks.sync.microsoft

import org.tasks.data.entity.CaldavAccount

interface MicrosoftClientProvider {
    suspend fun getService(account: CaldavAccount): MicrosoftService

    suspend fun hasCredentials(account: CaldavAccount): Boolean = !account.password.isNullOrBlank()
}
