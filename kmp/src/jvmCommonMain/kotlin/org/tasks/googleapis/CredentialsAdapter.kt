package org.tasks.googleapis

import com.google.api.client.http.HttpRequestInitializer

interface CredentialsAdapter : HttpRequestInitializer {
    suspend fun checkToken()
    suspend fun invalidateToken()
}
