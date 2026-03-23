package org.tasks.fcm

interface FcmTokenProvider {
    suspend fun getToken(): String?
}
