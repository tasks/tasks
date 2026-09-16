package org.tasks.fcm

interface FcmTokenProvider {
    suspend fun getToken(): String?

    val provider: String? get() = null

    companion object {
        const val PROVIDER_APNS = "apns"
    }
}
