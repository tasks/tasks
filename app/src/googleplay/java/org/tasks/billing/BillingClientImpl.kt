package org.tasks.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient.*
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProrationMode
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.Purchase.PurchasesResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.todoroo.andlib.utility.AndroidUtilities
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.tasks.BuildConfig
import org.tasks.analytics.Firebase
import timber.log.Timber

class BillingClientImpl(
    @ApplicationContext context: Context?,
    private val inventory: Inventory,
    private val firebase: Firebase
) : BillingClient, PurchasesUpdatedListener {
    private val billingClient = newBuilder(context!!).setListener(this).build()
    private var connected = false
    private var onPurchasesUpdated: OnPurchasesUpdated? = null

    /**
     * Query purchases across various use cases and deliver the result in a formalized way through a
     * listener
     */
    override fun queryPurchases() {
        val queryToExecute = Runnable {
            var purchases = Single.fromCallable { billingClient!!.queryPurchases(SkuType.INAPP) }
            if (areSubscriptionsSupported()) {
                purchases = Single.zip(
                    purchases,
                    Single.fromCallable { billingClient!!.queryPurchases(SkuType.SUBS) },
                    { iaps: PurchasesResult, subs: PurchasesResult ->
                        if (iaps.responseCode != BillingResponse.OK) {
                            return@zip iaps
                        }
                        if (subs.responseCode != BillingResponse.OK) {
                            return@zip subs
                        }
                        iaps.purchasesList.addAll(subs.purchasesList)
                        iaps
                    })
            }
            purchases
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { result: PurchasesResult -> onQueryPurchasesFinished(result) }
        }
        executeServiceRequest(queryToExecute)
    }

    /** Handle a result from querying of purchases and report an updated list to the listener  */
    private fun onQueryPurchasesFinished(result: PurchasesResult) {
        AndroidUtilities.assertMainThread()

        // Have we been disposed of in the meantime? If so, or bad result code, then quit
        if (billingClient == null || result.responseCode != BillingResponse.OK) {
            Timber.w(
                "Billing client was null or result code (%s) was bad - quitting",
                result.responseCode
            )
            return
        }
        Timber.d("Query inventory was successful.")

        // Update the UI and purchases inventory with new list of purchases
        inventory.clear()
        add(result.purchasesList)
    }

    override fun onPurchasesUpdated(
        @BillingResponse resultCode: Int, purchases: List<com.android.billingclient.api.Purchase>?
    ) {
        val success = resultCode == BillingResponse.OK
        if (success) {
            add(purchases ?: emptyList())
        }
        if (onPurchasesUpdated != null) {
            onPurchasesUpdated!!.onPurchasesUpdated(success)
        }
        val skus = purchases?.joinToString(";") { it.sku } ?: "null"
        Timber.i("onPurchasesUpdated(%s, %s)", BillingResponseToString(resultCode), skus)
        firebase.reportIabResult(resultCode, skus)
    }

    private fun add(purchases: List<com.android.billingclient.api.Purchase>) {
        inventory.add(purchases.map { Purchase(it) })
    }

    override fun initiatePurchaseFlow(
        activity: Activity, skuId: String, billingType: String, oldSku: String?
    ) {
        executeServiceRequest {
            billingClient!!.launchBillingFlow(
                activity,
                BillingFlowParams.newBuilder()
                    .setSku(skuId)
                    .setType(billingType)
                    .setOldSku(oldSku)
                    .setReplaceSkusProrationMode(ProrationMode.IMMEDIATE_WITH_TIME_PRORATION)
                    .build()
            )
        }
    }

    override fun addPurchaseCallback(onPurchasesUpdated: OnPurchasesUpdated) {
        this.onPurchasesUpdated = onPurchasesUpdated
    }

    private fun startServiceConnection(executeOnSuccess: Runnable?) {
        billingClient!!.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(@BillingResponse billingResponseCode: Int) {
                    Timber.d("onBillingSetupFinished(%s)", billingResponseCode)
                    if (billingResponseCode == BillingResponse.OK) {
                        connected = true
                        executeOnSuccess?.run()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Timber.d("onBillingServiceDisconnected()")
                    connected = false
                }
            })
    }

    private fun executeServiceRequest(runnable: Runnable) {
        if (connected) {
            runnable.run()
        } else {
            // If billing service was disconnected, we try to reconnect 1 time.
            // (feel free to introduce your retry policy here).
            startServiceConnection(runnable)
        }
    }

    /**
     * Checks if subscriptions are supported for current client
     *
     *
     * Note: This method does not automatically retry for RESULT_SERVICE_DISCONNECTED. It is only
     * used in unit tests and after queryPurchases execution, which already has a retry-mechanism
     * implemented.
     */
    private fun areSubscriptionsSupported(): Boolean {
        val responseCode = billingClient!!.isFeatureSupported(FeatureType.SUBSCRIPTIONS)
        if (responseCode != BillingResponse.OK) {
            Timber.d("areSubscriptionsSupported() got an error response: %s", responseCode)
        }
        return responseCode == BillingResponse.OK
    }

    override fun consume(sku: String) {
        check(BuildConfig.DEBUG)
        require(inventory.purchased(sku))
        val onConsumeListener =
            ConsumeResponseListener { responseCode: Int, purchaseToken1: String? ->
                Timber.d("onConsumeResponse(%s, %s)", responseCode, purchaseToken1)
                queryPurchases()
            }
        executeServiceRequest {
            billingClient!!.consumeAsync(
                inventory.getPurchase(sku)!!.purchaseToken, onConsumeListener
            )
        }
    }

    companion object {
        const val TYPE_SUBS = SkuType.SUBS
        fun BillingResponseToString(@BillingResponse response: Int): String {
            return when (response) {
                BillingResponse.FEATURE_NOT_SUPPORTED -> "FEATURE_NOT_SUPPORTED"
                BillingResponse.SERVICE_DISCONNECTED -> "SERVICE_DISCONNECTED"
                BillingResponse.OK -> "OK"
                BillingResponse.USER_CANCELED -> "USER_CANCELED"
                BillingResponse.SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE"
                BillingResponse.BILLING_UNAVAILABLE -> "BILLING_UNAVAILABLE"
                BillingResponse.ITEM_UNAVAILABLE -> "ITEM_UNAVAILABLE"
                BillingResponse.DEVELOPER_ERROR -> "DEVELOPER_ERROR"
                BillingResponse.ERROR -> "ERROR"
                BillingResponse.ITEM_ALREADY_OWNED -> "ITEM_ALREADY_OWNED"
                BillingResponse.ITEM_NOT_OWNED -> "ITEM_NOT_OWNED"
                else -> "Unknown"
            }
        }
    }
}