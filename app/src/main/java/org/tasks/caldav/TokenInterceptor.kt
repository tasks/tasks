package org.tasks.caldav

import okhttp3.Interceptor
import okhttp3.Response
import org.tasks.billing.Inventory

class TokenInterceptor(
        private val token: String,
        private val inventory: Inventory
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder().header(AUTHORIZATION, "Bearer $token")
        inventory.subscription?.let {
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