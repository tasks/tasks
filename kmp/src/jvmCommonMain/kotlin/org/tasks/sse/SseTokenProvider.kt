package org.tasks.sse

import org.tasks.fcm.FcmTokenProvider
import java.util.UUID

class SseTokenProvider : FcmTokenProvider {
    val token: String = UUID.randomUUID().toString()

    override suspend fun getToken(): String = token
}
