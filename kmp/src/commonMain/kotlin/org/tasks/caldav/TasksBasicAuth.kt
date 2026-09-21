package org.tasks.caldav

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.save
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.statement.bodyAsText
import io.ktor.util.AttributeKey
import io.ktor.util.encodeBase64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TasksBasicAuth(
    val user: String,
    token: String,
    private val tosVersion: Int,
    private val pushToken: String? = null,
    private val subscriptionInfo: SubscriptionInfo? = null,
) : HttpClientPlugin<Unit, TasksBasicAuth> {
    private val credentials = "Basic " + "$user:$token".encodeBase64()

    override val key = AttributeKey<TasksBasicAuth>("TasksBasicAuth")

    override fun prepare(block: Unit.() -> Unit) = this

    override fun install(plugin: TasksBasicAuth, scope: HttpClient) {
        scope.requestPipeline.intercept(HttpRequestPipeline.State) { plugin.addHeaders(context) }
        scope.plugin(HttpSend).intercept { request -> plugin.checkPaymentRequired(execute(request)) }
    }

    private fun addHeaders(request: HttpRequestBuilder) {
        request.headers[AUTHORIZATION] = credentials
        subscriptionInfo?.let {
            request.headers[SKU] = it.sku
            request.headers[TOKEN] = it.purchaseToken
        }
        request.headers[TOS_VERSION] = tosVersion.toString()
        pushToken?.let { request.headers[PUSH_TOKEN] = it }
    }

    private suspend fun checkPaymentRequired(call: HttpClientCall): HttpClientCall {
        if (call.response.status.value != 402) return call
        val saved = call.save()
        parsePurchaseTokenInUse(saved.response.bodyAsText())?.let { throw it }
        return saved
    }

    private fun parsePurchaseTokenInUse(body: String): PurchaseTokenInUseException? = try {
        val json = Json.parseToJsonElement(body).jsonObject
        if (json["error"]?.jsonPrimitive?.content == "purchase_token_in_use") {
            json["existing_account"]?.jsonPrimitive?.content?.let { PurchaseTokenInUseException(it) }
        } else null
    } catch (e: Exception) {
        Logger.d(TAG) { "Failed to parse 402 body: ${e.message}" }
        null
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
