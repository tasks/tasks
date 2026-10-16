package org.tasks.billing

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.tasks.http.KtorClientFactory
import org.tasks.http.errorCode
import org.tasks.http.jsonBody
import org.tasks.http.jsonField
import org.tasks.http.retryAfterMillis
import org.tasks.http.withClient

class StripeClientImpl(
    private val httpClientFactory: KtorClientFactory,
    private val caldavUrl: () -> String,
    private val json: Json,
    private val authorization: suspend () -> String? = { null },
    private val callTimeoutMillis: Long = CALL_TIMEOUT_MILLIS,
) : StripeClient {
    private val logger = Logger.withTag(TAG)

    @Serializable
    private data class PricesResponse(
        val prices: List<PriceResponse> = emptyList(),
    )

    @Serializable
    private data class PriceResponse(
        val price_id: String,
        val sku: String,
        val amount_cents: Int,
        val currency: String = "USD",
        val interval: String = StripePrice.INTERVAL_YEAR,
        val entitles: Boolean = false,
        val trial_days: Int = 0,
    )

    @Serializable
    private data class CheckoutRequest(val price_id: String? = null)

    @Serializable
    private data class CheckoutResponse(
        val code: String,
        val url: String,
        val expires_at: Long = 0L,
    )

    @Serializable
    private data class StatusResponse(
        val status: String = STATUS_PENDING,
        val purchase_jwt: String? = null,
        val refresh_token: String? = null,
        val sku: String? = null,
        val formatted_price: String? = null,
    )

    override suspend fun prices(): List<StripePrice>? = call("prices") { client ->
        val response = client.get(url("/stripe/prices"))
        val body = response.bodyAsText()
        if (!response.ok) {
            if (response.status.value == 503) {
                logger.i { "The card arm is unavailable: ${body.errorCode()}" }
            } else {
                logger.w { "Failed to load prices: HTTP ${response.status.value}" }
            }
            return@call null
        }
        json.decodeFromString(PricesResponse.serializer(), body).prices.map {
            StripePrice(
                priceId = it.price_id,
                sku = it.sku,
                amountCents = it.amount_cents,
                currency = it.currency,
                interval = it.interval,
                entitles = it.entitles,
                trialDays = it.trial_days,
            )
        }
    }

    override suspend fun createCheckout(priceId: String?): StripeCheckout {
        val session = authorization()
        val result = checkout(priceId, session)
        return if (result == null && session != null) {
            logger.i { "Session rejected, opening an anonymous checkout" }
            checkout(priceId, authorization = null) ?: StripeCheckout.Failed
        } else {
            result ?: StripeCheckout.Failed
        }
    }

    private suspend fun checkout(priceId: String?, authorization: String?): StripeCheckout? =
        call("checkout", fallback = StripeCheckout.Unavailable) { client ->
            val response = client.post(url("/stripe/checkout")) {
                authorization?.let { header(HttpHeaders.Authorization, it) }
                jsonBody(json, CheckoutRequest.serializer(), CheckoutRequest(priceId))
            }
            val body = response.bodyAsText()
            when (response.status.value) {
                in 200..299 -> {
                    val result = json.decodeFromString(CheckoutResponse.serializer(), body)
                    StripeCheckout.Started(
                        code = result.code,
                        url = result.url,
                        expiresAt = result.expires_at,
                    )
                }
                400 -> StripeCheckout.UnknownPrice
                401 -> null
                409 -> {
                    if (body.errorCode() != ERROR_SUBSCRIPTION_EXISTS) {
                        logger.w { "Unexpected 409 from checkout: ${body.errorCode()}" }
                    }
                    StripeCheckout.SubscriptionExists(product = body.jsonField("product"))
                }
                429 -> StripeCheckout.RateLimited
                503 -> StripeCheckout.Unavailable
                else -> {
                    logger.w { "Checkout failed: HTTP ${response.status.value}" }
                    StripeCheckout.Failed
                }
            }
        }

    override suspend fun pollCheckout(code: String): StripeCheckoutStatus =
        call("poll", fallback = StripeCheckoutStatus.Unavailable) { client ->
            val response = client.get(url("/stripe/checkout/$code"))
            val body = response.bodyAsText()
            when (response.status.value) {
                in 200..299 -> {
                    val result = json.decodeFromString(StatusResponse.serializer(), body)
                    val purchase = StripePurchase(
                        purchaseJwt = result.purchase_jwt ?: return@call pendingOrBroken(result),
                        refreshToken = result.refresh_token
                            ?: return@call pendingOrBroken(result),
                        sku = result.sku,
                        formattedPrice = result.formatted_price,
                    )
                    StripeCheckoutStatus.Complete(purchase)
                }
                402 -> StripeCheckoutStatus.SubscriptionInvalid
                404 -> StripeCheckoutStatus.Gone
                429 -> StripeCheckoutStatus.RateLimited(response.retryAfterMillis)
                503 -> StripeCheckoutStatus.Unavailable
                else -> {
                    logger.w { "Poll failed: HTTP ${response.status.value}" }
                    StripeCheckoutStatus.Failed
                }
            }
        }

    private fun pendingOrBroken(result: StatusResponse): StripeCheckoutStatus =
        if (result.status == STATUS_PENDING) {
            StripeCheckoutStatus.Pending
        } else {
            logger.w { "Checkout is ${result.status} but carries no purchase" }
            StripeCheckoutStatus.Failed
        }

    private fun url(path: String) = "${caldavUrl()}$path"

    private val HttpResponse.ok: Boolean get() = status.value in 200..299

    private suspend fun <T> call(what: String, block: suspend (HttpClient) -> T): T? =
        call(what, fallback = null) { block(it) }

    private suspend fun <T> call(
        what: String,
        fallback: T,
        block: suspend (HttpClient) -> T,
    ): T = try {
        httpClientFactory.withClient(callTimeoutMillis) { block(it) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        logger.e(e) { "Stripe $what failed" }
        fallback
    }

    companion object {
        private const val TAG = "StripeClient"
        private const val STATUS_PENDING = "pending"
        private const val ERROR_SUBSCRIPTION_EXISTS = "subscription_exists"
        private const val CALL_TIMEOUT_MILLIS = 20_000L
    }
}
