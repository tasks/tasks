package org.tasks.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient.*
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProrationMode
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase.PurchasesResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.querySkuDetails
import com.android.billingclient.api.consumePurchase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.tasks.BuildConfig
import org.tasks.analytics.Firebase
import timber.log.Timber
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class BillingClientImpl(
    @ApplicationContext context: Context?,
    private val inventory: Inventory,
    private val firebase: Firebase
) : BillingClient, PurchasesUpdatedListener {
    private val billingClient =
        newBuilder(context!!)
            .setListener(this)
            .enablePendingPurchases()
            .build()
    private var connected = false
    private var onPurchasesUpdated: OnPurchasesUpdated? = null

    override suspend fun queryPurchases() = try {
        executeServiceRequest {
            withContext(Dispatchers.IO + NonCancellable) {
                val subs = billingClient.queryPurchases(SkuType.SUBS)
                val iaps = billingClient.queryPurchases(SkuType.INAPP)
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
        Timber.e(e.message)
    }

    override fun onPurchasesUpdated(
        result: BillingResult, purchases: List<com.android.billingclient.api.Purchase>?
    ) {
        val success = result.success
        if (success) {
            add(purchases ?: emptyList())
        }
        onPurchasesUpdated?.onPurchasesUpdated(success)
        val skus = purchases?.joinToString(";") { it.sku } ?: "null"
        Timber.i("onPurchasesUpdated(${result.responseCodeString}, $skus)")
        firebase.reportIabResult(result, skus)
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
                    ?.takeIf { it.isNotEmpty() }
                    ?.firstOrNull()
                    ?: throw IllegalStateException("Sku $sku not found")
            val params = BillingFlowParams.newBuilder().setSkuDetails(skuDetails)
            oldPurchase?.let {
                params
                    .setOldSku(it.sku, it.purchaseToken)
                    .setReplaceSkusProrationMode(ProrationMode.IMMEDIATE_WITH_TIME_PRORATION)
            }
            if (activity is OnPurchasesUpdated) {
                onPurchasesUpdated = activity
            }
            billingClient.launchBillingFlow(activity, params.build())
        }
    }

    private suspend fun connect(): BillingResult =
        suspendCoroutine { cont ->
            billingClient.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (result.success) {
                            connected = true
                            cont.resumeWith(Result.success(result))
                        } else {
                            cont.resumeWithException(
                                IllegalStateException(result.responseCodeString)
                            )
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

        private val PurchasesResult.success: Boolean
            get() = responseCode == BillingResponseCode.OK

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

        private val PurchasesResult.responseCodeString: String
            get() = billingResult.responseCodeString

        private val PurchasesResult.purchases: List<com.android.billingclient.api.Purchase>
            get() = purchasesList ?: emptyList()
    }
}