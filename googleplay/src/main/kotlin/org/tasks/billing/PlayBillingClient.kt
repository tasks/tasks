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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tasks.TasksBuildConfig
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class PlayBillingClient(
    context: Context,
    private val purchases: PlayPurchases,
    private val reporting: Reporting,
    private val scope: CoroutineScope,
    private val onPurchasesUpdated: suspend (List<PlayPurchase>) -> Unit = {},
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

    private val acknowledging = Mutex()

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
            val failed = listOf(subs, iaps).firstOrNull { !it.success }
            if (failed != null) {
                reportIabResult(
                    result = "SUBS:${subs.responseCodeString} IAPs:${iaps.responseCodeString}",
                    state = STATE_QUERY_PURCHASES_FAILED,
                )
                throw failed.asException
            }
            purchases.replaceAll((subs.purchases + iaps.purchases).map { it.toPlayPurchase() })
        }
    }

    private suspend fun acknowledgePending(): Boolean {
        val pending = purchases.needingAcknowledgement
        pending.forEach { acknowledge(it) }
        return pending.isNotEmpty()
    }

    override suspend fun launchPurchaseFlow(
        activity: Activity,
        sku: String,
        subscription: Boolean,
        oldPurchaseToken: String?,
    ) {
        executeServiceRequest {
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
            logger.d { "launchBillingFlow: sku=$sku oldPurchaseToken=$oldPurchaseToken" }
            val result = billingClient.launchBillingFlow(activity, params.build())
            if (!result.success) {
                reportIabResult(result.responseCodeString, sku, STATE_LAUNCH_FAILED)
                throw result.asException
            }
        }
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
                state = STATE_ACKNOWLEDGE_FAILED,
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
            scope.launch {
                val purchased = updated.map { it.toPlayPurchase() }
                purchases.add(purchased)
                try {
                    acknowledging.withLock { acknowledgePending() }
                } catch (e: Exception) {
                    logger.w(e) { "Failed to acknowledge purchases after update" }
                }
                onPurchasesUpdated(purchased)
            }
            updated
                .filter { !it.isAcknowledged }
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
            reportIabResult(result.responseCodeString, state = STATE_PURCHASE_UPDATED_FAILED)
            scope.launch { onPurchasesUpdated(emptyList()) }
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

    private suspend fun connect(): BillingResult = suspendCancellableCoroutine { cont ->
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    connected = result.success
                    if (!cont.isActive) {
                        return
                    }
                    if (result.success) {
                        cont.resume(result)
                    } else {
                        cont.resumeWithException(result.asException)
                    }
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
            connecting.withLock {
                if (!connected) {
                    connect()
                }
            }
        }
        return runnable()
    }

    private fun reportIabResult(
        result: String,
        sku: String = "",
        state: String,
        orderId: String = "",
    ) = reporting.logEvent(
        AnalyticsEvents.BILLING_FLOW_RESULT,
        AnalyticsEvents.PARAM_RESULT to result,
        AnalyticsEvents.PARAM_SKU to sku,
        AnalyticsEvents.PARAM_STATE to state,
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
        private const val STATE_LAUNCH_FAILED = "LAUNCH_FAILED"
        private const val STATE_PURCHASE_UPDATED_FAILED = "PURCHASE_UPDATED_FAILED"
        private const val STATE_QUERY_PURCHASES_FAILED = "QUERY_PURCHASES_FAILED"
        private const val STATE_ACKNOWLEDGE_FAILED = "ACKNOWLEDGE_FAILED"

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
            get() = subscriptionOfferDetails?.firstOrNull()

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
