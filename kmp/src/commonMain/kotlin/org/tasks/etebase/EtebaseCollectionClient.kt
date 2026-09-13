package org.tasks.etebase

import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar

interface EtebaseCollectionClient {
    suspend fun makeCollection(name: String, color: Int): String
    suspend fun updateCollection(calendar: CaldavCalendar, name: String, color: Int): String
    suspend fun deleteCollection(calendar: CaldavCalendar): String
}

interface EtebaseCollectionClientProvider {
    suspend fun forAccount(account: CaldavAccount): EtebaseCollectionClient
}
