package org.tasks.billing

import android.app.Activity

interface BillingClient {
    suspend fun queryPurchases()
    suspend fun consume(sku: String)
    suspend fun initiatePurchaseFlow(
        activity: Activity,
        sku: String,
        skuType: String,
        oldPurchase: Purchase? = null
    )
}