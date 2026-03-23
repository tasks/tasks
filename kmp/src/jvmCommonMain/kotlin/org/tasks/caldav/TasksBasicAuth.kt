package org.tasks.caldav

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
        return chain.proceed(builder.build())
    }

    data class SubscriptionInfo(val sku: String, val purchaseToken: String)

    companion object {
        private const val AUTHORIZATION = "Authorization"
        private const val SKU = "tasks-sku"
        private const val TOKEN = "tasks-token"
        private const val TOS_VERSION = "tasks-tos-version"
        private const val PUSH_TOKEN = "X-Push-Token"
    }
}
