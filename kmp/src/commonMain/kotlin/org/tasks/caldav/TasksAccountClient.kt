package org.tasks.caldav

import kotlinx.serialization.json.JsonObject

interface TasksAccountClient : AutoCloseable {
    suspend fun generateNewPassword(description: String?): JsonObject?
    suspend fun deletePassword(id: Int)
    suspend fun registerPushToken(token: String)
    suspend fun unregisterPushToken(token: String)
    suspend fun getAccount(): String?
    suspend fun regenerateInboundEmail(): JsonObject?
    suspend fun setInboundCalendar(calendar: String?): JsonObject?
}
