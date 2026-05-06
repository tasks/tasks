package org.tasks.caldav

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

class TasksBasicAuth(
    val user: String,
    token: String,
    private val tosVersion: Int,
    private val pushToken: String? = null,
    private val subscriptionInfo: SubscriptionInfo? = null,
) : Interceptor {
    private val credentials = Credentials.basic(user, token, Charsets.UTF_8)

    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder().header(AUTHORIZATION, credentials)
        subscriptionInfo?.let {
            builder.header(SKU, it.sku)
            builder.header(TOKEN, it.purchaseToken)
        }
        builder.header(TOS_VERSION, tosVersion.toString())
        pushToken?.let { builder.header(PUSH_TOKEN, it) }
        val response = chain.proceed(builder.build())
        if (response.code == 402) {
            parsePurchaseTokenInUse(response)?.let { throw it }
        }
        return response
    }

    private fun parsePurchaseTokenInUse(response: Response): PurchaseTokenInUseException? {
        return try {
            val body = response.peekBody(1024).string()
            val json = Json.parseToJsonElement(body).jsonObject
            if (json["error"]?.jsonPrimitive?.content == "purchase_token_in_use") {
                json["existing_account"]?.jsonPrimitive?.content?.let {
                    response.close()
                    PurchaseTokenInUseException(it)
                }
            } else null
        } catch (e: Exception) {
            Logger.d(TAG) { "Failed to parse 402 body: ${e.message}" }
            null
        }
    }

    data class SubscriptionInfo(val sku: String, val purchaseToken: String)

    companion object {
        private const val TAG = "TasksBasicAuth"
        private const val AUTHORIZATION = "Authorization"
        private const val SKU = "tasks-sku"
        private const val TOKEN = "tasks-token"
        private const val TOS_VERSION = "tasks-tos-version"
        private const val PUSH_TOKEN = "X-Push-Token"
    }
}
