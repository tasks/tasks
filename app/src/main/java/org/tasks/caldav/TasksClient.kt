package org.tasks.caldav

import at.bitfire.cert4android.CustomCertManager
import at.bitfire.dav4jvm.exception.HttpException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject

class TasksClient constructor(
        provider: CaldavClientProvider,
        certManager: CustomCertManager,
        httpClient: OkHttpClient,
        private val httpUrl: HttpUrl?
) : CaldavClient(provider, certManager, httpClient, httpUrl) {
    suspend fun generateNewPassword(description: String?): JSONObject? = withContext(Dispatchers.IO) {
        val url = httpUrl?.resolve(ENDPOINT_PASSWORDS) ?: return@withContext null
        httpClient
                .newCall(Request.Builder()
                .post(FormBody.Builder()
                        .apply {
                            if (!description.isNullOrBlank()) {
                                add(FORM_DESCRIPTION, description)
                            }
                        }
                        .build()
                )
                .url(url)
                .build())
                .execute()
                .use { response ->
                    if (!response.isSuccessful) {
                        throw HttpException(response)
                    }
                    response.body?.use { body -> JSONObject(body.string()) }
                }
    }

    suspend fun deletePassword(id: Int) = withContext(Dispatchers.IO) {
        val url = httpUrl?.resolve(ENDPOINT_PASSWORDS) ?: return@withContext false
        httpClient
                .newCall(Request.Builder()
                .delete(FormBody.Builder().add(FORM_SESSION_ID, id.toString()).build())
                .url(url)
                .build())
                .execute()
                .use { response ->
                    if (!response.isSuccessful) {
                        throw HttpException(response)
                    }
                }
    }

    suspend fun getAppPasswords(): JSONObject? = withContext(Dispatchers.IO) {
        val url = httpUrl?.resolve(ENDPOINT_PASSWORDS) ?: return@withContext null
        httpClient
                .newCall(Request.Builder()
                .get()
                .url(url)
                .build())
                .execute()
                .use { response ->
                    if (!response.isSuccessful) {
                        throw HttpException(response)
                    }
                    response.body?.use { body -> JSONObject(body.string()) }
                }
    }

    companion object {
        private const val ENDPOINT_PASSWORDS = "/app-passwords"
        private const val FORM_DESCRIPTION = "description"
        private const val FORM_SESSION_ID = "session_id"
    }
}