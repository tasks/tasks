package org.tasks.billing

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.tasks.auth.TasksServerEnvironment
import org.tasks.http.OkHttpClientFactory

class DesktopLinkServiceImpl(
    private val httpClientFactory: OkHttpClientFactory,
    private val serverEnvironment: TasksServerEnvironment,
    private val subscriptionProvider: SubscriptionProvider,
    private val json: Json,
) : DesktopLinkService {
    private val logger = Logger.withTag("DesktopLinkServiceImpl")

    @Serializable
    private data class ConfirmRequest(
        val code: String,
        val product: String,
        val purchase_token: String,
        val formatted_price: String? = null,
    )

    override suspend fun confirmLink(code: String): Boolean {
        val sub = subscriptionProvider.subscription.firstOrNull()
        if (sub == null) {
            logger.w { "No active subscription found" }
            return false
        }
        val purchaseToken = sub.purchaseToken
        if (purchaseToken == null) {
            logger.w { "No purchase token available" }
            return false
        }

        return withContext(Dispatchers.IO) {
            val formattedPrice = subscriptionProvider.getFormattedPrice(sub.sku)
            val client = httpClientFactory.newClient()
            val jsonBody = json.encodeToString(
                ConfirmRequest.serializer(),
                ConfirmRequest(
                    code = code,
                    product = sub.sku,
                    purchase_token = purchaseToken,
                    formatted_price = formattedPrice,
                )
            )
            val body = jsonBody.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${serverEnvironment.caldavUrl}/desktop/confirm")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            response.use {
                if (!it.isSuccessful) {
                    logger.w { "Desktop link confirmation failed: ${it.code}" }
                }
                it.isSuccessful
            }
        }
    }
}
