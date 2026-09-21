package org.tasks.sse

import org.tasks.fcm.FcmTokenProvider
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SseTokenProvider : FcmTokenProvider {
    @OptIn(ExperimentalUuidApi::class)
    val token: String = Uuid.random().toString()

    override suspend fun getToken(): String = token
}
