package org.tasks.billing

import android.app.Activity
import android.content.Context
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
import com.android.billingclient.api.Purchase.PurchaseState
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.consumePurchase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.tasks.BuildConfig
import org.tasks.analytics.Firebase
import org.tasks.jobs.WorkManager
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class BillingClientImpl(
    @ApplicationContext context: Context?,
    private val inventory: Inventory,
    private val firebase: Firebase,
    private val workManager: WorkManager
) : BillingClient, PurchasesUpdatedListener {
    private val billingClient =
        newBuilder(context!!)
            .setListener(this)
            .enablePendingPurchases()
            .build()
    private var connected = false
    private var onPurchased: (() -> Unit)? = null

    override suspend fun getSkus(skus: List<String>): List<Sku> =
        executeServiceRequest {
            val productList = skus.map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(it)
                    .setProductType(ProductType.SUBS)
                    .build()
            }
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            val productDetailsResult = withContext(Dispatchers.IO) {
                suspendCoroutine { cont ->
                    billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                        cont.resume(billingResult to productDetailsList)
                    }
                }
            }

            productDetailsResult.first.let {
                if (!it.success) {
                    throw IllegalStateException(it.responseCodeString)
                }
            }

            productDetailsResult.second?.map { productDetails ->
                Sku(
                    productId = productDetails.productId,
                    price = productDetails.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                        ?: productDetails.oneTimePurchaseOfferDetails?.formattedPrice
                        ?: ""
                )
            } ?: emptyList()
        }

    override suspend fun queryPurchases(throwError: Boolean) = try {
        executeServiceRequest {
            withContext(Dispatchers.IO + NonCancellable) {
                val subsParams = QueryPurchasesParams.newBuilder()
                    .setProductType(ProductType.SUBS)
                    .build()
                val iapsParams = QueryPurchasesParams.newBuilder()
                    .setProductType(ProductType.INAPP)
                    .build()

                val subs = suspendCoroutine { cont ->
                    billingClient.queryPurchasesAsync(subsParams) { billingResult, purchases ->
                        cont.resume(PurchasesResult(billingResult, purchases))
                    }
                }
                val iaps = suspendCoroutine { cont ->
                    billingClient.queryPurchasesAsync(iapsParams) { billingResult, purchases ->
                        cont.resume(PurchasesResult(billingResult, purchases))
                    }
                }

                if (subs.success || iaps.success) {
                    withContext(Dispatchers.Main) {
                        inventory.clear()
                        add(subs.purchases + iaps.purchases)
                    }
                } else {
                    Timber.e("SUBS: ${subs.responseCodeString} IAPs: ${iaps.responseCodeString}")
                }
            }
        }
    } catch (e: IllegalStateException) {
        if (throwError) {
            throw e
        } else {
            Timber.e(e.message)
        }
    }

    override fun onPurchasesUpdated(
        result: BillingResult,
        purchases: List<com.android.billingclient.api.Purchase>?,
    ) {
        val success = result.success
        if (success) {
            add(purchases ?: emptyList())
            onPurchased?.invoke()
            purchases
                ?.filter { !it.isAcknowledged }
                ?.forEach {
                    firebase.reportIabResult(
                        result.responseCodeString,
                        it.products.joinToString(","),
                        it.purchaseState.purchaseStateString,
                        it.orderId ?: "",
                    )
                }
        }
        workManager.updatePurchases()
    }

    private fun add(purchases: List<com.android.billingclient.api.Purchase>) {
        inventory.add(purchases.map { Purchase(it) })
    }

    override suspend fun initiatePurchaseFlow(
        activity: Activity,
        sku: String,
        skuType: String,
        oldPurchase: Purchase?,
        onPurchased: (() -> Unit)?,
    ) {
        executeServiceRequest {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(sku)
                    .setProductType(skuType)
                    .build()
            )
            val queryParams = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            val productDetailsResult = withContext(Dispatchers.IO) {
                suspendCoroutine { cont ->
                    billingClient.queryProductDetailsAsync(queryParams) { billingResult, productDetailsList ->
                        cont.resume(billingResult to productDetailsList)
                    }
                }
            }

            productDetailsResult.first.let {
                if (!it.success) {
                    throw IllegalStateException(it.responseCodeString)
                }
            }

            val productDetails = productDetailsResult.second?.firstOrNull()
                ?: throw IllegalStateException("Product $sku not found")

            val productDetailsParamsBuilder = ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)

            // For subscriptions (including legacy subscriptions), we need to provide an offer token
            if (skuType == ProductType.SUBS) {
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                    ?: throw IllegalStateException("No offer token found for subscription $sku")
                productDetailsParamsBuilder.setOfferToken(offerToken)
            }

            val productDetailsParams = productDetailsParamsBuilder.build()

            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))

            oldPurchase?.let {
                params.setSubscriptionUpdateParams(
                    SubscriptionUpdateParams.newBuilder()
                        .setOldPurchaseToken(it.purchaseToken)
                        .setSubscriptionReplacementMode(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION)
                        .build()
                )
            }

            this@BillingClientImpl.onPurchased = onPurchased
            billingClient.launchBillingFlow(activity, params.build())
        }
    }

    override suspend fun acknowledge(purchase: Purchase) {
        if (purchase.needsAcknowledgement) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            withContext(Dispatchers.IO) {
                suspendCoroutine { cont ->
                    billingClient.acknowledgePurchase(params) {
                        Timber.d("acknowledge: ${it.responseCodeString} $purchase")
                        cont.resume(it)
                    }
                }
            }
        }
    }

    private suspend fun connect(): BillingResult =
        suspendCancellableCoroutine { cont ->
            billingClient.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (result.success) {
                            connected = true
                            if (cont.isActive) {
                                cont.resumeWith(Result.success(result))
                            }
                        } else {
                            connected = false
                            if (cont.isActive) {
                                cont.resumeWithException(
                                    IllegalStateException(result.responseCodeString)
                                )
                            }
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        Timber.d("onBillingServiceDisconnected()")
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

    override suspend fun consume(sku: String) {
        check(BuildConfig.DEBUG)
        val purchase = inventory.getPurchase(sku)
        require(purchase != null)
        executeServiceRequest {
            val result = billingClient.consumePurchase(
                ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
            )
            Timber.d("consume purchase: ${result.billingResult.responseCodeString}")
            queryPurchases(throwError = false)
        }
    }

    private data class PurchasesResult(
        val billingResult: BillingResult,
        val purchasesList: List<com.android.billingclient.api.Purchase>
    ) {
        val success: Boolean
            get() = billingResult.responseCode == BillingResponseCode.OK

        val responseCodeString: String
            get() = billingResult.responseCodeString

        val purchases: List<com.android.billingclient.api.Purchase>
            get() = purchasesList
    }

    companion object {
        const val TYPE_SUBS = ProductType.SUBS
        const val STATE_PURCHASED = PurchaseState.PURCHASED

        private val BillingResult.success: Boolean
            get() = responseCode == BillingResponseCode.OK

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
                PurchaseState.UNSPECIFIED_STATE -> "UNSPECIFIED_STATE"
                PurchaseState.PURCHASED -> "PURCHASED"
                PurchaseState.PENDING -> "PENDING"
                else -> this.toString()
            }
    }
}