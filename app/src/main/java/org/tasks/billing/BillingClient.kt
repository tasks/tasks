package org.tasks.billing

import android.app.Activity

interface BillingClient {
    fun queryPurchases()
    fun consume(sku: String)
    fun initiatePurchaseFlow(activity: Activity, sku: String, skuType: String, oldSku: String?)
    fun addPurchaseCallback(onPurchasesUpdated: OnPurchasesUpdated)
}