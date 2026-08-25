package org.tasks.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.logging.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.BuildConfig
import timber.log.Timber
import javax.inject.Inject

class AndroidOpenRouterClientProvider @Inject constructor(
    private val gate: AiGate,
    private val credentials: AiCredentialStore,
) : OpenRouterClientProvider {

    override suspend fun getService(): OpenRouterService? = withContext(Dispatchers.IO) {
        if (!gate.canCall()) return@withContext null
        val apiKey = credentials.getApiKey() ?: return@withContext null
        OpenRouterService(newClient(apiKey))
    }

    override suspend fun validateKey(rawKey: String): List<ModelInfo> = withContext(Dispatchers.IO) {
        OpenRouterService(newClient(rawKey.trim())).listModels()
    }

    private fun newClient(apiKey: String) = HttpClient(Android) {
        installOpenRouter(
            apiKey = apiKey,
            logLevel = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.HEADERS,
            log = { Timber.d(it) },
        )
    }
}
