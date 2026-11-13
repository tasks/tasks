package org.tasks.billing

import android.app.Activity
import android.content.Context
import co.touchlab.kermit.Logger
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClient.newBuilder
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.consumePurchase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.tasks.TasksBuildConfig
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.AnalyticsEvents.BillingFlow
import org.tasks.analytics.Reporting
import org.tasks.analytics.logBillingFlow
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.seconds

class PlayBillingClient(
    context: Context,
    private val purchases: PlayPurchases,
    private val reporting: Reporting,
    private val scope: CoroutineScope,
    private val onPurchasesAdded: suspend (List<PlayPurchase>) -> Unit = {},
    private val onPurchaseFlowFinished: suspend () -> Unit = {},
) : PlayBilling, PurchasesUpdatedListener {
    private val logger = Logger.withTag("PlayBillingClient")

    private val billingClient = newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    @Volatile
    private var connected = false

    private val connecting = Mutex()

    private var connection: CompletableDeferred<BillingResult>? = null

    private val acknowledging = Mutex()

    private class Launch(val sku: String) {
        @Volatile
        var returned = false
    }

    private val launch = AtomicReference<Launch?>(null)

    private val reported = mutableSetOf<String>()

    override suspend fun products(skus: List<String>): List<PlayProductInfo> =
        executeServiceRequest {
            queryProductDetails(skus, ProductType.SUBS).map { it.toProductInfo() }
        }

    override suspend fun queryPurchases() {
        refreshPurchases()
        acknowledging.withLock {
            if (acknowledgePending()) {
                refreshPurchases()
            }
        }
    }

    private suspend fun refreshPurchases() = executeServiceRequest {
        withContext(Dispatchers.IO + NonCancellable) {
            val subsQuery = async { queryPurchasesOfType(ProductType.SUBS) }
            val iapsQuery = async { queryPurchasesOfType(ProductType.INAPP) }
            val subs = subsQuery.await()
            val iaps = iapsQuery.await()
            if (!subs.success || !iaps.success) {
                reportIabResult(
                    result = "SUBS:${subs.responseCodeString} IAPs:${iaps.responseCodeString}",
                    state = BillingFlow.QUERY_PURCHASES_FAILED,
                )
            }
            if (!subs.success) {
                throw subs.asException
            }
            val oneTime = if (iaps.success) {
                iaps.purchases.map { it.toPlayPurchase() }
            } else {
                purchases.state.value.purchases.values.filterNot { it.isProSubscription }
            }
            purchases.replaceAll(subs.purchases.map { it.toPlayPurchase() } + oneTime)
        }
    }

    private suspend fun acknowledgePending(): Boolean {
        var acknowledged = false
        purchases.needingAcknowledgement.forEach {
            try {
                acknowledge(it)
                acknowledged = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.w(e) { "Failed to acknowledge $it" }
            }
        }
        return acknowledged
    }

    override suspend fun launchPurchaseFlow(
        activity: Activity,
        sku: String,
        subscription: Boolean,
        oldPurchaseToken: String?,
    ) {
        val params = try {
            executeServiceRequest { billingFlowParams(sku, subscription, oldPurchaseToken) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportIabResult(e.message, sku, BillingFlow.PURCHASE_FAILED)
            throw e
        }
        logger.d { "launchBillingFlow: sku=$sku oldPurchaseToken=$oldPurchaseToken" }
        val started = Launch(sku)
        launch.set(started)
        val result = try {
            billingClient.launchBillingFlow(activity, params)
        } catch (e: Exception) {
            launch.compareAndSet(started, null)
            reportIabResult(e.message, sku, BillingFlow.LAUNCH_FAILED)
            throw e
        }
        if (!result.success) {
            launch.compareAndSet(started, null)
            reportIabResult(result.responseCodeString, sku, BillingFlow.LAUNCH_FAILED)
            throw result.asException
        }
        reportIabResult(result.responseCodeString, sku, BillingFlow.LAUNCHED)
        started.returned = true
    }

    private suspend fun billingFlowParams(
        sku: String,
        subscription: Boolean,
        oldPurchaseToken: String?,
    ): BillingFlowParams {
        val productType = if (subscription) ProductType.SUBS else ProductType.INAPP
        val productDetails = queryProductDetails(listOf(sku), productType).firstOrNull()
            ?: throw PlayBillingUnrecoverableException("Product $sku not found")
        val productParams = ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
        if (subscription) {
            productParams.setOfferToken(
                productDetails.offer?.offerToken
                    ?: throw PlayBillingUnrecoverableException("No offer token for $sku")
            )
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams.build()))
        oldPurchaseToken?.let {
            params.setSubscriptionUpdateParams(
                SubscriptionUpdateParams.newBuilder()
                    .setOldPurchaseToken(it)
                    .setSubscriptionReplacementMode(
                        SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION
                    )
                    .build()
            )
        }
        return params.build()
    }

    override suspend fun acknowledge(purchase: PlayPurchase) {
        if (!purchase.needsAcknowledgement) {
            return
        }
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val result = withContext(Dispatchers.IO) {
            suspendCoroutine { cont ->
                billingClient.acknowledgePurchase(params) { cont.resume(it) }
            }
        }
        logger.d { "acknowledge: ${result.responseCodeString} $purchase" }
        if (!result.success) {
            reportIabResult(
                result = result.responseCodeString,
                sku = purchase.sku,
                state = BillingFlow.ACKNOWLEDGE_FAILED,
                orderId = purchase.orderId.orEmpty(),
            )
            throw result.asException
        }
    }

    override suspend fun consume(purchase: PlayPurchase) {
        check(TasksBuildConfig.DEBUG) { "Purchases are only consumable in a debug build" }
        executeServiceRequest {
            val result = billingClient.consumePurchase(
                ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
            )
            logger.d { "consume purchase: ${result.billingResult.responseCodeString}" }
            queryPurchases()
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchaseList: List<Purchase>?) {
        if (result.success) {
            val updated = purchaseList.orEmpty()
            launch.getAndUpdate { started ->
                started?.takeIf { updated.none { started.sku in it.products } }
            }
            scope.launch {
                val known = updated.mapNotNull { purchases.get(it.products.first())?.purchaseToken }
                val purchased = purchases.add(updated.map { it.toPlayPurchase() })
                    .filter { it.isPurchased && it.purchaseToken !in known }
                try {
                    queryPurchases()
                } catch (e: Exception) {
                    logger.w(e) { "Failed to refresh purchases after update" }
                    try {
                        acknowledging.withLock { acknowledgePending() }
                    } catch (e: Exception) {
                        logger.w(e) { "Failed to acknowledge purchases after update" }
                    }
                }
                onPurchaseFlowFinished()
                if (purchased.isNotEmpty()) {
                    onPurchasesAdded(purchased)
                }
            }
            updated
                .filter { !it.isAcknowledged }
                .filter { firstReport(it.purchaseToken, it.purchaseState) }
                .forEach {
                    reportIabResult(
                        result = result.responseCodeString,
                        sku = it.products.joinToString(","),
                        state = it.purchaseState.purchaseStateString,
                        orderId = it.orderId.orEmpty(),
                    )
                }
        } else {
            logger.w { "onPurchasesUpdated: ${result.responseCodeString}: ${result.debugMessage}" }
            val started = launch.get()
            if (started?.returned == true && launch.compareAndSet(started, null)) {
                reportIabResult(
                    result.responseCodeString,
                    started.sku,
                    BillingFlow.PURCHASE_UPDATED_FAILED,
                )
            }
            scope.launch { onPurchaseFlowFinished() }
        }
    }

    private suspend fun queryProductDetails(
        skus: List<String>,
        productType: String,
    ): List<ProductDetails> {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                skus.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(productType)
                        .build()
                }
            )
            .build()
        val (billingResult, result) = withContext(Dispatchers.IO) {
            suspendCoroutine { cont ->
                billingClient.queryProductDetailsAsync(params) { billingResult, result ->
                    cont.resume(billingResult to result)
                }
            }
        }
        if (!billingResult.success) {
            throw billingResult.asException
        }
        return result.productDetailsList
    }

    private suspend fun queryPurchasesOfType(productType: String): PurchasesResult {
        val params = QueryPurchasesParams.newBuilder().setProductType(productType).build()
        return suspendCoroutine { cont ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                cont.resume(PurchasesResult(billingResult, purchases))
            }
        }
    }

    private suspend fun connect() {
        val attempt = connecting.withLock {
            connection?.takeIf { it.isActive }
                ?: CompletableDeferred<BillingResult>().also {
                    connection = it
                    startConnection(it)
                }
        }
        val result = withTimeoutOrNull(CONNECT_TIMEOUT) { attempt.await() }
            ?: throw PlayBillingUnavailableException(CONNECT_TIMED_OUT)
        if (!result.success) {
            throw result.asException
        }
    }

    private fun startConnection(attempt: CompletableDeferred<BillingResult>) {
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    connected = result.success
                    attempt.complete(result)
                }

                override fun onBillingServiceDisconnected() {
                    logger.d { "onBillingServiceDisconnected()" }
                    connected = false
                }
            }
        )
    }

    private suspend fun <T> executeServiceRequest(runnable: suspend () -> T): T {
        if (!connected) {
            connect()
        }
        return runnable()
    }

    private fun firstReport(purchaseToken: String, purchaseState: Int): Boolean =
        synchronized(reported) {
            reported.add("$purchaseToken:$purchaseState")
        }

    private fun reportIabResult(
        result: String?,
        sku: String = "",
        state: String,
        orderId: String = "",
    ) = reporting.logBillingFlow(
        BillingProvider.GOOGLE_PLAY,
        state,
        sku = sku,
        result = result ?: AnalyticsEvents.Checkout.FAILED,
        AnalyticsEvents.PARAM_ORDER_ID to orderId,
    )

    private data class PurchasesResult(
        private val billingResult: BillingResult,
        val purchases: List<Purchase>,
    ) {
        val success: Boolean get() = billingResult.success

        val responseCodeString: String get() = billingResult.responseCodeString

        val asException: IllegalStateException get() = billingResult.asException
    }

    companion object {
        private val CONNECT_TIMEOUT = 30.seconds
        private const val CONNECT_TIMED_OUT = "CONNECT_TIMED_OUT"

        private val unrecoverable = setOf(
            BillingResponseCode.BILLING_UNAVAILABLE,
            BillingResponseCode.FEATURE_NOT_SUPPORTED,
            BillingResponseCode.DEVELOPER_ERROR,
            BillingResponseCode.ITEM_UNAVAILABLE,
        )

        private val transient = setOf(
            BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingResponseCode.SERVICE_DISCONNECTED,
            BillingResponseCode.ERROR,
        )

        private val ProductDetails.offer: ProductDetails.SubscriptionOfferDetails?
            get() = subscriptionOfferDetails.orEmpty().preferredOffer(
                offerId = { it.offerId },
                startsFree = { it.trialPhase != null },
            )

        internal fun <T> List<T>.preferredOffer(
            offerId: (T) -> String?,
            startsFree: (T) -> Boolean,
        ): T? = firstOrNull(startsFree)
            ?: firstOrNull { offerId(it) == null }
            ?: firstOrNull()

        private val ProductDetails.SubscriptionOfferDetails.recurringPhase: ProductDetails.PricingPhase?
            get() = pricingPhases.pricingPhaseList.maxByOrNull { it.priceAmountMicros }

        private val ProductDetails.SubscriptionOfferDetails.trialPhase: ProductDetails.PricingPhase?
            get() = pricingPhases.pricingPhaseList.firstOrNull { it.priceAmountMicros == 0L }

        internal fun ProductDetails.toProductInfo(): PlayProductInfo {
            val offer = offer
            val phase = offer?.recurringPhase
            return PlayProductInfo(
                productId = productId,
                formattedPrice = phase?.formattedPrice
                    ?: oneTimePurchaseOfferDetails?.formattedPrice
                    ?: "",
                priceAmountMicros = phase?.priceAmountMicros
                    ?: oneTimePurchaseOfferDetails?.priceAmountMicros
                    ?: 0L,
                priceCurrencyCode = phase?.priceCurrencyCode
                    ?: oneTimePurchaseOfferDetails?.priceCurrencyCode
                    ?: DEFAULT_CURRENCY,
                trialPeriod = offer?.trialPhase?.billingPeriod,
            )
        }

        private const val DEFAULT_CURRENCY = "USD"

        private val BillingResult.success: Boolean
            get() = responseCode == BillingResponseCode.OK

        private val BillingResult.asException: IllegalStateException
            get() = when (responseCode) {
                in unrecoverable -> PlayBillingUnrecoverableException(responseCodeString)
                in transient -> PlayBillingUnavailableException(responseCodeString)
                else -> IllegalStateException(responseCodeString)
            }

        val BillingResult.responseCodeString: String
            get() = when (responseCode) {
                BillingResponseCode.FEATURE_NOT_SUPPORTED -> "FEATURE_NOT_SUPPORTED"
                BillingResponseCode.SERVICE_DISCONNECTED -> "SERVICE_DISCONNECTED"
                BillingResponseCode.OK -> "OK"
                BillingResponseCode.USER_CANCELED -> "USER_CANCELED"
                BillingResponseCode.SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE"
                BillingResponseCode.BILLING_UNAVAILABLE -> "BILLING_UNAVAILABLE"
                BillingResponseCode.ITEM_UNAVAILABLE -> "ITEM_UNAVAILABLE"
                BillingResponseCode.DEVELOPER_ERROR -> "DEVELOPER_ERROR"
                BillingResponseCode.ERROR -> "ERROR"
                BillingResponseCode.ITEM_ALREADY_OWNED -> "ITEM_ALREADY_OWNED"
                BillingResponseCode.ITEM_NOT_OWNED -> "ITEM_NOT_OWNED"
                else -> "UNKNOWN"
            }

        val Int.purchaseStateString: String
            get() = when (this) {
                Purchase.PurchaseState.UNSPECIFIED_STATE -> "UNSPECIFIED_STATE"
                Purchase.PurchaseState.PURCHASED -> "PURCHASED"
                Purchase.PurchaseState.PENDING -> "PENDING"
                else -> toString()
            }
    }
}
