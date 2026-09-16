package org.tasks.caldav

import at.bitfire.dav4jvm.ktor.exception.HttpException
import at.bitfire.dav4jvm.ktor.resolve
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.tasks.http.translateExceptions

class TasksClient(
        httpClient: HttpClient,
        private val httpUrl: Url?
) : CaldavClient(httpClient, httpUrl), TasksAccountClient {
    override suspend fun generateNewPassword(description: String?): JsonObject? = translateExceptions {
        withContext(Dispatchers.IO) {
            val url = httpUrl?.resolve(ENDPOINT_PASSWORDS) ?: return@withContext null
            httpClient
                .post(url) {
                    setBody(FormDataContent(parameters {
                        if (!description.isNullOrBlank()) {
                            append(FORM_DESCRIPTION, description)
                        }
                    }))
                }
                .checkSuccess()
                .json()
        }
    }

    override suspend fun deletePassword(id: Int): Unit = translateExceptions {
        withContext(Dispatchers.IO) {
            val url = httpUrl?.resolve(ENDPOINT_PASSWORDS) ?: return@withContext
            httpClient
                .delete(url) {
                    setBody(FormDataContent(parameters { append(FORM_SESSION_ID, id.toString()) }))
                }
                .checkSuccess()
        }
    }

    override suspend fun registerPushToken(token: String): Unit = translateExceptions {
        withContext(Dispatchers.IO) {
            val url = httpUrl?.resolve(ENDPOINT_PUSH_TOKEN) ?: return@withContext
            httpClient
                .post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(mapOf("token" to JsonPrimitive(token))).toString())
                }
                .checkSuccess()
        }
    }

    override suspend fun unregisterPushToken(token: String): Unit = translateExceptions {
        withContext(Dispatchers.IO) {
            val url = httpUrl?.resolve(ENDPOINT_PUSH_TOKEN) ?: return@withContext
            httpClient
                .delete(url) {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(mapOf("token" to JsonPrimitive(token))).toString())
                }
                .checkSuccess()
        }
    }

    override suspend fun getAccount(): String? = translateExceptions {
        withContext(Dispatchers.IO) {
            val url = httpUrl?.resolve(ENDPOINT_ACCOUNT) ?: return@withContext null
            httpClient
                .get(url)
                .checkSuccess()
                .bodyAsText()
        }
    }

    override suspend fun regenerateInboundEmail(): JsonObject? = translateExceptions {
        withContext(Dispatchers.IO) {
            val url = httpUrl?.resolve(ENDPOINT_INBOUND_EMAIL) ?: return@withContext null
            httpClient
                .post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(mapOf("regenerate" to JsonPrimitive(true))).toString())
                }
                .checkSuccess()
                .json()
        }
    }

    override suspend fun setInboundCalendar(calendar: String?): JsonObject? = translateExceptions {
        withContext(Dispatchers.IO) {
            val url = httpUrl?.resolve(ENDPOINT_INBOUND_EMAIL) ?: return@withContext null
            httpClient
                .post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        JsonObject(mapOf(
                            "calendar" to if (calendar != null) JsonPrimitive(calendar) else JsonNull
                        )).toString()
                    )
                }
                .checkSuccess()
                .json()
        }
    }

    private suspend fun HttpResponse.checkSuccess(): HttpResponse {
        if (!status.isSuccess()) {
            throw HttpException.fromResponse(this)
        }
        return this
    }

    private suspend fun HttpResponse.json(): JsonObject =
        Json.parseToJsonElement(bodyAsText()).jsonObject

    companion object {
        private const val ENDPOINT_ACCOUNT = "/api/account"
        private const val ENDPOINT_PASSWORDS = "/app-passwords"
        private const val ENDPOINT_PUSH_TOKEN = "/push-token"
        private const val ENDPOINT_INBOUND_EMAIL = "/inbound-email"
        private const val FORM_DESCRIPTION = "description"
        private const val FORM_SESSION_ID = "session_id"
    }
}
