package org.tasks.billing

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
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
    private data class ChangeRequest(
        val price_id: String,
        val purchase_jwt: String? = null,
    )

    @Serializable
    private data class ChangeResponse(val entitles: Boolean = false)

    @Serializable
    private data class CheckoutResponse(
        val code: String,
        val url: String,
        val expires_at: Long = 0L,
    )

    @Serializable
    private data class RestoreRequest(val email: String)

    @Serializable
    private data class VerifyRequest(val email: String, val code: String)

    @Serializable
    private data class PurchaseResponse(
        val status: String = STATUS_PENDING,
        val purchase_jwt: String? = null,
        val refresh_token: String? = null,
        val sku: String? = null,
        val formatted_price: String? = null,
    ) {
        val purchase: StripePurchase?
            get() {
                val jwt = purchase_jwt ?: return null
                val refreshToken = refresh_token ?: return null
                return StripePurchase(jwt, refreshToken, sku, formatted_price)
            }
    }

    @Serializable
    private data class PortalRequest(val purchase_jwt: String)

    @Serializable
    private data class PortalResponse(val url: String? = null)

    override suspend fun prices(): List<StripePrice>? = call("prices") { client ->
        val response = client.get(url("/stripe/prices"))
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
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
        checkout(priceId, session)?.let { return it }
        if (session == null) return StripeCheckout.Failed
        logger.i { "Session rejected, opening an anonymous checkout" }
        return checkout(priceId, authorization = null) ?: StripeCheckout.Failed
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

    override suspend fun changePlan(priceId: String, purchaseJwt: String?): StripeChange =
        call("change", fallback = StripeChange.Unavailable) { client ->
            val session = authorization()
            val response = client.post(url("/stripe/change")) {
                session?.let { header(HttpHeaders.Authorization, it) }
                jsonBody(
                    json,
                    ChangeRequest.serializer(),
                    ChangeRequest(price_id = priceId, purchase_jwt = purchaseJwt),
                )
            }
            val body = response.bodyAsText()
            when (response.status.value) {
                in 200..299 -> StripeChange.Changed(
                    entitles = json.decodeFromString(ChangeResponse.serializer(), body).entitles,
                )
                400 -> if (body.errorCode() == ERROR_INVALID_PURCHASE_JWT) {
                    StripeChange.PurchaseGone
                } else {
                    StripeChange.UnknownPrice
                }
                401 -> {
                    logger.w {
                        "Change refused: session=${session != null} receipt=${purchaseJwt != null}"
                    }
                    StripeChange.NoProofOfPurchase
                }
                404 -> if (body.errorCode() == ERROR_UNKNOWN_PURCHASE) {
                    StripeChange.PurchaseGone
                } else {
                    StripeChange.NotLinked
                }
                409 -> StripeChange.Cancelled
                429 -> StripeChange.RateLimited
                503 -> StripeChange.Unavailable
                else -> {
                    logger.w { "Plan change failed: HTTP ${response.status.value}" }
                    StripeChange.Failed
                }
            }
        }

    override suspend fun pollCheckout(code: String): StripeCheckoutStatus =
        call("poll", fallback = StripeCheckoutStatus.Unavailable) { client ->
            val response = client.get(url("/stripe/checkout/$code"))
            val body = response.bodyAsText()
            when (response.status.value) {
                in 200..299 -> {
                    val result = json.decodeFromString(PurchaseResponse.serializer(), body)
                    result.purchase
                        ?.let { StripeCheckoutStatus.Complete(it) }
                        ?: pendingOrBroken(result)
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

    private fun pendingOrBroken(result: PurchaseResponse): StripeCheckoutStatus =
        if (result.status == STATUS_PENDING) {
            StripeCheckoutStatus.Pending
        } else {
            logger.w { "Checkout is ${result.status} but carries no purchase" }
            StripeCheckoutStatus.Failed
        }

    override suspend fun requestRestoreCode(email: String): StripeRestoreRequest =
        call("restore", fallback = StripeRestoreRequest.Unavailable) { client ->
            val response = client.post(url("/stripe/restore")) {
                jsonBody(json, RestoreRequest.serializer(), RestoreRequest(email))
            }
            when (response.status.value) {
                in 200..299 -> StripeRestoreRequest.Sent
                400 -> StripeRestoreRequest.InvalidEmail
                429 -> StripeRestoreRequest.RateLimited
                503 -> StripeRestoreRequest.Unavailable
                else -> {
                    logger.w { "Restore request failed: HTTP ${response.status.value}" }
                    StripeRestoreRequest.Failed
                }
            }
        }

    override suspend fun verifyRestoreCode(email: String, code: String): StripeRestore =
        call("verify", fallback = StripeRestore.Unavailable) { client ->
            val response = client.post(url("/stripe/restore/verify")) {
                jsonBody(json, VerifyRequest.serializer(), VerifyRequest(email, code))
            }
            val body = response.bodyAsText()
            when (response.status.value) {
                in 200..299 -> {
                    val purchase = json
                        .decodeFromString(PurchaseResponse.serializer(), body)
                        .purchase
                    if (purchase == null) {
                        logger.w { "Restore response has no purchase" }
                        StripeRestore.Failed
                    } else {
                        StripeRestore.Restored(purchase)
                    }
                }
                400 -> if (body.errorCode() == ERROR_INVALID_EMAIL) {
                    StripeRestore.InvalidEmail
                } else {
                    StripeRestore.InvalidCode
                }
                402 -> StripeRestore.SubscriptionInvalid
                429 -> StripeRestore.TooManyAttempts
                503 -> StripeRestore.Unavailable
                else -> {
                    logger.w { "Restore verify failed: HTTP ${response.status.value}" }
                    StripeRestore.Failed
                }
            }
        }

    override suspend fun portalUrl(purchaseJwt: String?): String? = call("portal") { client ->
        val response = client.post(url("/stripe/portal")) {
            authorization()?.let { header(HttpHeaders.Authorization, it) }
            purchaseJwt?.let { jsonBody(json, PortalRequest.serializer(), PortalRequest(it)) }
        }
        if (!response.status.isSuccess()) {
            logger.w { "Failed to open the customer portal: HTTP ${response.status.value}" }
            return@call null
        }
        json.decodeFromString(PortalResponse.serializer(), response.bodyAsText()).url
    }

    private fun url(path: String) = "${caldavUrl()}$path"

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
        private const val ERROR_INVALID_EMAIL = "invalid_email"
        private const val ERROR_INVALID_PURCHASE_JWT = "invalid_purchase_jwt"
        private const val ERROR_UNKNOWN_PURCHASE = "unknown_purchase"
        private const val CALL_TIMEOUT_MILLIS = 20_000L
    }
}
