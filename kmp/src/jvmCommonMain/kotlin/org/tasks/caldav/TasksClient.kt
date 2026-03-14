package org.tasks.caldav

import at.bitfire.dav4jvm.okhttp.exception.HttpException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class TasksClient(
        provider: CaldavClientProvider,
        httpClient: OkHttpClient,
        private val httpUrl: HttpUrl?
) : CaldavClient(provider, httpClient, httpUrl) {
    suspend fun generateNewPassword(description: String?): JsonObject? = withContext(Dispatchers.IO) {
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
                    response.body.use { body -> Json.parseToJsonElement(body.string()).jsonObject }
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

    suspend fun registerPushToken(token: String) = withContext(Dispatchers.IO) {
        val url = httpUrl?.resolve(ENDPOINT_PUSH_TOKEN) ?: return@withContext
        val body = JsonObject(mapOf("token" to JsonPrimitive(token))).toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        httpClient
            .newCall(Request.Builder().post(body).url(url).build())
            .execute()
            .use { response ->
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
            }
    }

    suspend fun unregisterPushToken(token: String) = withContext(Dispatchers.IO) {
        val url = httpUrl?.resolve(ENDPOINT_PUSH_TOKEN) ?: return@withContext
        val body = JsonObject(mapOf("token" to JsonPrimitive(token))).toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        httpClient
            .newCall(Request.Builder().delete(body).url(url).build())
            .execute()
            .use { response ->
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
            }
    }

    suspend fun getAccount(): String? = withContext(Dispatchers.IO) {
        val url = httpUrl?.resolve(ENDPOINT_ACCOUNT) ?: return@withContext null
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
                    response.body.use { body -> body.string() }
                }
    }

    suspend fun regenerateInboundEmail(): JsonObject? = withContext(Dispatchers.IO) {
        val url = httpUrl?.resolve(ENDPOINT_INBOUND_EMAIL) ?: return@withContext null
        val body = JsonObject(mapOf("regenerate" to JsonPrimitive(true))).toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        httpClient
            .newCall(Request.Builder()
                .post(body)
                .url(url)
                .build())
            .execute()
            .use { response ->
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
                response.body.use { body -> Json.parseToJsonElement(body.string()).jsonObject }
            }
    }

    suspend fun setInboundCalendar(calendar: String?): JsonObject? = withContext(Dispatchers.IO) {
        val url = httpUrl?.resolve(ENDPOINT_INBOUND_EMAIL) ?: return@withContext null
        val body = JsonObject(mapOf(
            "calendar" to if (calendar != null) JsonPrimitive(calendar) else JsonNull
        )).toString().toRequestBody(JSON_MEDIA_TYPE)
        httpClient
            .newCall(Request.Builder()
                .post(body)
                .url(url)
                .build())
            .execute()
            .use { response ->
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
                response.body.use { body -> Json.parseToJsonElement(body.string()).jsonObject }
            }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private const val ENDPOINT_ACCOUNT = "/api/account"
        private const val ENDPOINT_PASSWORDS = "/app-passwords"
        private const val ENDPOINT_PUSH_TOKEN = "/push-token"
        private const val ENDPOINT_INBOUND_EMAIL = "/inbound-email"
        private const val FORM_DESCRIPTION = "description"
        private const val FORM_SESSION_ID = "session_id"
    }
}
