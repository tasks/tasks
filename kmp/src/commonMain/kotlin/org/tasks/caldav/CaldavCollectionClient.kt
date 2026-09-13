package org.tasks.caldav

import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar

interface CaldavCollectionClient : AutoCloseable {
    suspend fun makeCollection(displayName: String, color: Int, icon: String?): String
    suspend fun updateCollection(displayName: String, color: Int, icon: String?)
    suspend fun deleteCollection()
    suspend fun share(account: CaldavAccount, href: String)
    suspend fun removePrincipal(account: CaldavAccount, calendar: CaldavCalendar, href: String)
}
