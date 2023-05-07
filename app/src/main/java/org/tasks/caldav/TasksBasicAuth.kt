package org.tasks.caldav

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response
import org.tasks.billing.Inventory

class TasksBasicAuth(
        val user: String,
        token: String,
        private val inventory: Inventory
) : Interceptor {
    private val credentials = Credentials.basic(user, token, Charsets.UTF_8)

    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder().header(AUTHORIZATION, credentials)
        inventory.subscription.value?.let {
            builder.header(SKU, it.sku)
            builder.header(TOKEN, it.purchaseToken)
        }
        return chain.proceed(builder.build())
    }

    companion object {
        private const val AUTHORIZATION = "Authorization"
        private const val SKU = "tasks-sku"
        private const val TOKEN = "tasks-token"
    }
}