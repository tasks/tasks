package org.tasks.sse

import org.tasks.fcm.FcmTokenProvider
import org.tasks.preferences.InstallId

class SseTokenProvider(private val installId: InstallId) : FcmTokenProvider {
    override suspend fun getToken(): String = installId.get()
}
