package org.tasks.etebase

import org.tasks.data.entity.CaldavCalendar

interface EtebaseAccountClient {
    fun getSession(): String
    suspend fun logout()
    suspend fun makeCollection(name: String, color: Int): String
    suspend fun updateCollection(calendar: CaldavCalendar, name: String, color: Int): String
    suspend fun deleteCollection(calendar: CaldavCalendar): String
}
