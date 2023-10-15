package org.tasks.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.BillingClient.newBuilder
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProrationMode
import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase.PurchaseState
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryPurchasesAsync
import com.android.billingclient.api.querySkuDetails
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
    private var onPurchasesUpdated: OnPurchasesUpdated? = null

    override suspend fun queryPurchases(throwError: Boolean) = try {
        executeServiceRequest {
            withContext(Dispatchers.IO + NonCancellable) {
                val subs = billingClient.queryPurchasesAsync(SkuType.SUBS)
                val iaps = billingClient.queryPurchasesAsync(SkuType.INAPP)
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
        result: BillingResult, purchases: List<com.android.billingclient.api.Purchase>?
    ) {
        val success = result.success
        if (success) {
            add(purchases ?: emptyList())
        }
        workManager.updatePurchases()
        onPurchasesUpdated?.onPurchasesUpdated(success)
        purchases?.forEach {
            firebase.reportIabResult(
                result.responseCodeString,
                it.skus.joinToString(","),
                it.purchaseState.purchaseStateString
            )
        }
    }

    private fun add(purchases: List<com.android.billingclient.api.Purchase>) {
        inventory.add(purchases.map { Purchase(it) })
    }

    override suspend fun initiatePurchaseFlow(
        activity: Activity,
        sku: String,
        skuType: String,
        oldPurchase: Purchase?
    ) {
        executeServiceRequest {
            val skuDetailsResult = withContext(Dispatchers.IO) {
                billingClient.querySkuDetails(
                    SkuDetailsParams.newBuilder().setSkusList(listOf(sku)).setType(skuType)
                        .build()
                )
            }
            skuDetailsResult.billingResult.let {
                if (!it.success) {
                    throw IllegalStateException(it.responseCodeString)
                }
            }
            val skuDetails =
                skuDetailsResult
                    .skuDetailsList
                    ?.firstOrNull()
                    ?: throw IllegalStateException("Sku $sku not found")
            val params = BillingFlowParams.newBuilder().setSkuDetails(skuDetails)
            oldPurchase?.let {
                params.setSubscriptionUpdateParams(
                    SubscriptionUpdateParams.newBuilder()
                        .setOldSkuPurchaseToken(it.purchaseToken)
                        .setReplaceSkusProrationMode(ProrationMode.IMMEDIATE_WITH_TIME_PRORATION)
                        .build()
                )
            }
            if (activity is OnPurchasesUpdated) {
                onPurchasesUpdated = activity
            }
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

    private suspend fun executeServiceRequest(runnable: suspend () -> Unit) {
        if (!connected) {
            connect()
        }
        runnable()
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
            queryPurchases()
        }
    }

    companion object {
        const val TYPE_SUBS = SkuType.SUBS
        const val STATE_PURCHASED = PurchaseState.PURCHASED

        private val PurchasesResult.success: Boolean
            get() = billingResult.responseCode == BillingResponseCode.OK

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

        private val PurchasesResult.responseCodeString: String
            get() = billingResult.responseCodeString

        private val PurchasesResult.purchases: List<com.android.billingclient.api.Purchase>
            get() = purchasesList
    }
}